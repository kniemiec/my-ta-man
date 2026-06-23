package com.mytaman.service;

import com.mytaman.model.Project;
import com.mytaman.model.ProjectState;
import com.mytaman.storage.Frontmatter;
import com.mytaman.storage.IdSequence;
import com.mytaman.storage.MarkdownStore;
import com.mytaman.storage.ProjectRepository;
import com.mytaman.storage.TaskRepository;
import com.mytaman.storage.VaultPaths;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Project business logic: validation, id assignment, field-merge updates, and the
 * "don't delete a project that still has tasks" guard (HTTP 409).
 */
public final class ProjectService {

    private static final Set<String> PATCHABLE = Set.of("name", "description", "due", "state");

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final VaultPaths paths;
    private final IdSequence ids;

    public ProjectService(ProjectRepository projects, TaskRepository tasks,
                          VaultPaths paths, IdSequence ids) {
        this.projects = projects;
        this.tasks = tasks;
        this.paths = paths;
        this.ids = ids;
    }

    public List<Project> list() {
        return projects.findAll();
    }

    /** Project plus its full task list. */
    public Project get(String id) {
        Project p = projects.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
        p.setTasks(tasks.findByProject(id));
        return p;
    }

    public Project create(CreateProjectRequest req) {
        if (req == null || req.name == null || req.name.isBlank()) {
            throw new ValidationException("Project name is required");
        }
        ProjectState state = req.state != null ? req.state : ProjectState.NEW;

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("id", ids.nextProjectId());
        meta.put("name", req.name.trim());
        meta.put("state", state.wire());
        if (req.due != null) {
            meta.put("due", req.due.toString());
        }
        meta.put("created", LocalDate.now().toString());

        String body = req.description != null ? req.description : "";
        Path dir = projects.create(VaultPaths.slug(req.name), new Frontmatter(meta, body));
        return projects.readProject(dir);
    }

    public Project patch(String id, Map<String, Object> changes) {
        Path dir = projects.dirForId(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
        if (changes == null || changes.isEmpty()) {
            return get(id);
        }
        for (String key : changes.keySet()) {
            if (!PATCHABLE.contains(key)) {
                throw new ValidationException("Unknown field: " + key);
            }
        }

        Path file = paths.projectFile(dir);
        Frontmatter fm = MarkdownStore.read(file);
        Map<String, Object> meta = fm.metadata();
        String body = fm.body();

        if (changes.containsKey("name")) {
            String name = asString(changes.get("name"));
            if (name == null || name.isBlank()) {
                throw new ValidationException("Project name cannot be blank");
            }
            meta.put("name", name.trim());
        }
        if (changes.containsKey("state")) {
            meta.put("state", parseProjectState(changes.get("state")).wire());
        }
        if (changes.containsKey("due")) {
            Object due = changes.get("due");
            if (due == null || asString(due).isBlank()) {
                meta.remove("due");
            } else {
                meta.put("due", parseDate(asString(due)).toString());
            }
        }
        if (changes.containsKey("description")) {
            Object desc = changes.get("description");
            body = desc == null ? "" : asString(desc);
        }

        MarkdownStore.write(file, new Frontmatter(meta, body));
        return get(id);
    }

    public void delete(String id) {
        Path dir = projects.dirForId(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
        if (projects.countTasks(dir) > 0) {
            throw new ConflictException("Project still has tasks; move or delete them first");
        }
        projects.deleteDir(dir);
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static ProjectState parseProjectState(Object o) {
        try {
            return ProjectState.fromWire(asString(o));
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    private static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date (expected YYYY-MM-DD): " + s);
        }
    }
}
