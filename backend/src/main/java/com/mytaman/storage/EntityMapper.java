package com.mytaman.storage;

import com.mytaman.model.Project;
import com.mytaman.model.ProjectState;
import com.mytaman.model.Task;
import com.mytaman.model.TaskState;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Converts between {@link Frontmatter} (raw YAML map + body) and the domain models.
 *
 * <p>Mapping is tolerant: unknown/extra frontmatter keys (e.g. tags a user adds in
 * Obsidian) are ignored on read and preserved on field-merge writes since those mutate
 * the raw map in place rather than rebuilding it.
 */
public final class EntityMapper {

    private EntityMapper() {
    }

    public static Task toTask(Frontmatter fm, String projectId) {
        Task t = new Task();
        t.setId(str(fm, "id"));
        t.setName(str(fm, "name"));
        t.setState(parseTaskState(str(fm, "state")));
        t.setDue(date(fm, "due"));
        t.setCreated(date(fm, "created"));
        Map<String, String> sections = BodyParser.parse(fm.body());
        t.setDescription(blankToNull(sections.get(BodyParser.DESCRIPTION)));
        t.setProgress(blankToNull(sections.get(BodyParser.PROGRESS)));
        t.setProjectId(projectId);
        return t;
    }

    public static Project toProject(Frontmatter fm) {
        Project p = new Project();
        p.setId(str(fm, "id"));
        p.setName(str(fm, "name"));
        p.setState(parseProjectState(str(fm, "state")));
        p.setDue(date(fm, "due"));
        p.setCreated(date(fm, "created"));
        Map<String, String> sections = BodyParser.parse(fm.body());
        p.setDescription(blankToNull(sections.get(BodyParser.DESCRIPTION)));
        p.setProgress(blankToNull(sections.get(BodyParser.PROGRESS)));
        return p;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String str(Frontmatter fm, String key) {
        Object v = fm.metadata().get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static LocalDate date(Frontmatter fm, String key) {
        String v = str(fm, key);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static TaskState parseTaskState(String v) {
        try {
            return TaskState.fromWire(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ProjectState parseProjectState(String v) {
        try {
            return ProjectState.fromWire(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
