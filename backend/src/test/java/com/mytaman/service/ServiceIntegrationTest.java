package com.mytaman.service;

import com.mytaman.model.Project;
import com.mytaman.model.ProjectState;
import com.mytaman.model.Task;
import com.mytaman.model.TaskState;
import com.mytaman.storage.IdSequence;
import com.mytaman.storage.ProjectRepository;
import com.mytaman.storage.TaskRepository;
import com.mytaman.storage.VaultPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceIntegrationTest {

    @TempDir
    Path vault;

    private ProjectService projects;
    private TaskService tasks;
    private VaultPaths paths;

    @BeforeEach
    void setUp() {
        paths = new VaultPaths(vault);
        IdSequence ids = new IdSequence(paths.countersFile());
        ProjectRepository projectRepo = new ProjectRepository(paths);
        TaskRepository taskRepo = new TaskRepository(paths, projectRepo);
        projects = new ProjectService(projectRepo, taskRepo, paths, ids);
        tasks = new TaskService(taskRepo, ids);
    }

    @Test
    void createProjectWritesFolderAndMetadataFile() throws IOException {
        CreateProjectRequest req = new CreateProjectRequest();
        req.name = "Project Alpha";
        req.description = "Background rationale.";
        Project created = projects.create(req);

        assertEquals("PROJ-1", created.getId());
        assertEquals(ProjectState.NEW, created.getState());

        Path dir = vault.resolve("project-alpha");
        assertTrue(Files.isDirectory(dir));
        String content = Files.readString(dir.resolve("_project.md"));
        assertTrue(content.contains("id: PROJ-1"));
        assertTrue(content.contains("Background rationale."));
    }

    @Test
    void createTaskInProjectAndListByProject() {
        CreateProjectRequest p = new CreateProjectRequest();
        p.name = "Alpha";
        String pid = projects.create(p).getId();

        CreateTaskRequest t = new CreateTaskRequest();
        t.name = "Fix login bug";
        t.projectId = pid;
        Task created = tasks.create(t);

        assertEquals("TASK-1", created.getId());
        assertEquals(pid, created.getProjectId());
        assertEquals(TaskState.NEW, created.getState());

        List<Task> inProject = tasks.list(pid);
        assertEquals(1, inProject.size());
        assertEquals("TASK-1", inProject.get(0).getId());
    }

    @Test
    void taskWithoutProjectGoesToInbox() {
        CreateTaskRequest t = new CreateTaskRequest();
        t.name = "Buy domain";
        Task created = tasks.create(t);

        assertEquals(VaultPaths.INBOX_ID, created.getProjectId());
        assertTrue(Files.isDirectory(paths.inboxDir()));
        assertEquals(1, tasks.list("inbox").size());
    }

    @Test
    void patchStateLeavesBodyUntouched() {
        CreateTaskRequest t = new CreateTaskRequest();
        t.name = "Write docs";
        t.description = "Original body content.";
        String id = tasks.create(t).getId();

        Task patched = tasks.patch(id, Map.of("state", "in-progress"));
        assertEquals(TaskState.IN_PROGRESS, patched.getState());
        assertEquals("Original body content.", patched.getDescription());
    }

    @Test
    void patchDescriptionLeavesMetadataUntouched() {
        CreateTaskRequest t = new CreateTaskRequest();
        t.name = "Write docs";
        t.due = LocalDate.parse("2026-07-01");
        String id = tasks.create(t).getId();

        Task patched = tasks.patch(id, Map.of("description", "New body."));
        assertEquals("New body.", patched.getDescription());
        assertEquals(LocalDate.parse("2026-07-01"), patched.getDue());
        assertEquals("Write docs", patched.getName());
    }

    @Test
    void clearingDueRemovesTheField() {
        CreateTaskRequest t = new CreateTaskRequest();
        t.name = "Task";
        t.due = LocalDate.parse("2026-07-01");
        String id = tasks.create(t).getId();

        java.util.HashMap<String, Object> changes = new java.util.HashMap<>();
        changes.put("due", null);
        Task patched = tasks.patch(id, changes);
        assertNull(patched.getDue());
    }

    @Test
    void moveTaskBetweenProjects() {
        CreateProjectRequest a = new CreateProjectRequest();
        a.name = "Alpha";
        String alpha = projects.create(a).getId();
        CreateProjectRequest b = new CreateProjectRequest();
        b.name = "Beta";
        String beta = projects.create(b).getId();

        CreateTaskRequest t = new CreateTaskRequest();
        t.name = "Movable";
        t.projectId = alpha;
        String id = tasks.create(t).getId();

        Task moved = tasks.move(id, beta);
        assertEquals(beta, moved.getProjectId());
        assertEquals(0, tasks.list(alpha).size());
        assertEquals(1, tasks.list(beta).size());
    }

    @Test
    void deleteProjectWithTasksReturns409() {
        CreateProjectRequest p = new CreateProjectRequest();
        p.name = "Alpha";
        String pid = projects.create(p).getId();
        CreateTaskRequest t = new CreateTaskRequest();
        t.name = "Blocker";
        t.projectId = pid;
        tasks.create(t);

        assertThrows(ConflictException.class, () -> projects.delete(pid));
    }

    @Test
    void deleteEmptyProjectSucceeds() {
        CreateProjectRequest p = new CreateProjectRequest();
        p.name = "Empty";
        String pid = projects.create(p).getId();
        projects.delete(pid);
        assertThrows(NotFoundException.class, () -> projects.get(pid));
    }

    @Test
    void getMissingTaskThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> tasks.get("TASK-999"));
    }

    @Test
    void createTaskRequiresName() {
        CreateTaskRequest t = new CreateTaskRequest();
        t.name = "  ";
        assertThrows(ValidationException.class, () -> tasks.create(t));
    }

    @Test
    void obsidianEditIsReflectedImmediately() throws IOException {
        CreateTaskRequest t = new CreateTaskRequest();
        t.name = "Edit me";
        t.description = "before";
        String id = tasks.create(t).getId();

        // Simulate a hand-edit in Obsidian: rewrite the body directly on disk.
        Path file = paths.inboxDir().resolve("edit-me.md");
        String onDisk = Files.readString(file).replace("before", "after edit");
        Files.writeString(file, onDisk);

        // No cache: the next read serves the new content.
        assertEquals("after edit", tasks.get(id).getDescription());
    }
}
