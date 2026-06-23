package com.mytaman.service;

import com.mytaman.model.Task;
import com.mytaman.model.TaskState;
import com.mytaman.storage.EntityMapper;
import com.mytaman.storage.Frontmatter;
import com.mytaman.storage.IdSequence;
import com.mytaman.storage.MarkdownStore;
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
 * Task business logic: validation, id assignment, defaults, field-merge updates, moves.
 * The merge re-reads the file and mutates only the touched keys so edits never disturb
 * untouched fields or the body (and vice versa).
 */
public final class TaskService {

    private static final Set<String> PATCHABLE = Set.of("name", "description", "due", "state");

    private final TaskRepository tasks;
    private final IdSequence ids;

    public TaskService(TaskRepository tasks, IdSequence ids) {
        this.tasks = tasks;
        this.ids = ids;
    }

    public List<Task> list(String projectFilter) {
        if (projectFilter == null || projectFilter.isBlank()) {
            return tasks.findAll();
        }
        return tasks.findByProject(projectFilter);
    }

    public Task get(String id) {
        return tasks.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found: " + id));
    }

    public Task create(CreateTaskRequest req) {
        if (req == null || req.name == null || req.name.isBlank()) {
            throw new ValidationException("Task name is required");
        }
        String projectId = (req.projectId == null || req.projectId.isBlank())
                ? VaultPaths.INBOX_ID : req.projectId;
        if (!VaultPaths.INBOX_ID.equalsIgnoreCase(projectId)
                && tasks.dirForProjectId(projectId).isEmpty()) {
            throw new ValidationException("Unknown project: " + projectId);
        }
        TaskState state = req.state != null ? req.state : TaskState.NEW;

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("id", ids.nextTaskId());
        meta.put("name", req.name.trim());
        meta.put("state", state.wire());
        if (req.due != null) {
            meta.put("due", req.due.toString());
        }
        meta.put("created", LocalDate.now().toString());

        String body = req.description != null ? req.description : "";
        Frontmatter fm = new Frontmatter(meta, body);
        return tasks.create(projectId, VaultPaths.slug(req.name), fm);
    }

    /** Field-merge update. {@code changes} carries only the fields to modify. */
    public Task patch(String id, Map<String, Object> changes) {
        Path file = tasks.pathForId(id)
                .orElseThrow(() -> new NotFoundException("Task not found: " + id));
        if (changes == null || changes.isEmpty()) {
            return get(id);
        }
        for (String key : changes.keySet()) {
            if (!PATCHABLE.contains(key)) {
                throw new ValidationException("Unknown field: " + key);
            }
        }

        Frontmatter fm = MarkdownStore.read(file);
        Map<String, Object> meta = fm.metadata();
        String body = fm.body();

        if (changes.containsKey("name")) {
            String name = asString(changes.get("name"));
            if (name == null || name.isBlank()) {
                throw new ValidationException("Task name cannot be blank");
            }
            meta.put("name", name.trim());
        }
        if (changes.containsKey("state")) {
            meta.put("state", parseTaskState(changes.get("state")).wire());
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

    public Task move(String id, String targetProjectId) {
        Path file = tasks.pathForId(id)
                .orElseThrow(() -> new NotFoundException("Task not found: " + id));
        if (targetProjectId == null || targetProjectId.isBlank()) {
            throw new ValidationException("Target project is required");
        }
        if (!VaultPaths.INBOX_ID.equalsIgnoreCase(targetProjectId)
                && tasks.dirForProjectId(targetProjectId).isEmpty()) {
            throw new ValidationException("Unknown project: " + targetProjectId);
        }
        Path moved = tasks.move(file, targetProjectId);
        String projectId = VaultPaths.INBOX_ID.equalsIgnoreCase(targetProjectId)
                ? VaultPaths.INBOX_ID : targetProjectId;
        return EntityMapper.toTask(MarkdownStore.read(moved), projectId);
    }

    public void delete(String id) {
        Path file = tasks.pathForId(id)
                .orElseThrow(() -> new NotFoundException("Task not found: " + id));
        tasks.delete(file);
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static TaskState parseTaskState(Object o) {
        try {
            return TaskState.fromWire(asString(o));
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
