package com.mytaman.storage;

import com.mytaman.model.Task;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Persistence for tasks. A task is a {@code .md} file (other than {@code _project.md})
 * inside a project folder or the {@code Inbox/} folder. The containing folder determines
 * the task's {@code projectId} ({@code "inbox"} for project-less tasks).
 */
public final class TaskRepository {

    private final VaultPaths paths;
    private final ProjectRepository projects;

    public TaskRepository(VaultPaths paths, ProjectRepository projects) {
        this.paths = paths;
        this.projects = projects;
    }

    public List<Task> findAll() {
        List<Task> result = new ArrayList<>();
        for (Path dir : taskDirs()) {
            String projectId = projectIdForDir(dir);
            for (Path file : taskFiles(dir)) {
                result.add(EntityMapper.toTask(MarkdownStore.read(file), projectId));
            }
        }
        return result;
    }

    public List<Task> findByProject(String projectId) {
        Path dir = dirForProjectId(projectId).orElse(null);
        if (dir == null || !Files.isDirectory(dir)) {
            return List.of();
        }
        List<Task> result = new ArrayList<>();
        for (Path file : taskFiles(dir)) {
            result.add(EntityMapper.toTask(MarkdownStore.read(file), projectId));
        }
        return result;
    }

    public Optional<Task> findById(String id) {
        return pathForId(id).map(file ->
                EntityMapper.toTask(MarkdownStore.read(file), projectIdForDir(file.getParent())));
    }

    /** Locate the file backing a task id by scanning frontmatter (tolerates renamed files). */
    public Optional<Path> pathForId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (Path dir : taskDirs()) {
            for (Path file : taskFiles(dir)) {
                String fileId = EntityMapper.toTask(MarkdownStore.read(file), null).getId();
                if (id.equals(fileId)) {
                    return Optional.of(file);
                }
            }
        }
        return Optional.empty();
    }

    /** Create a task file in the folder for {@code projectId} ({@code "inbox"} allowed). */
    public Task create(String projectId, String baseSlug, Frontmatter fm) {
        Path dir = resolveOrCreateDir(projectId);
        Path file = paths.uniqueFile(dir, baseSlug);
        MarkdownStore.write(file, fm);
        return EntityMapper.toTask(MarkdownStore.read(file), projectId);
    }

    /** Move a task's file into another project's folder (or Inbox); returns the new path. */
    public Path move(Path file, String targetProjectId) {
        Path targetDir = resolveOrCreateDir(targetProjectId);
        return MarkdownStore.move(file, targetDir);
    }

    public void delete(Path file) {
        MarkdownStore.delete(file);
    }

    /** The folder for a project id, or the Inbox; empty if the project doesn't exist. */
    public Optional<Path> dirForProjectId(String projectId) {
        if (isInbox(projectId)) {
            return Optional.of(paths.inboxDir());
        }
        return projects.dirForId(projectId);
    }

    private Path resolveOrCreateDir(String projectId) {
        Path dir = dirForProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown project: " + projectId));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create folder: " + dir, e);
        }
        return dir;
    }

    private String projectIdForDir(Path dir) {
        if (dir.equals(paths.inboxDir())) {
            return VaultPaths.INBOX_ID;
        }
        String id = projects.idForDir(dir);
        return id != null ? id : VaultPaths.INBOX_ID;
    }

    private static boolean isInbox(String projectId) {
        return projectId == null || VaultPaths.INBOX_ID.equalsIgnoreCase(projectId);
    }

    /** All folders that may contain tasks: every project folder plus Inbox (if present). */
    private List<Path> taskDirs() {
        List<Path> dirs = new ArrayList<>();
        try {
            if (Files.isDirectory(paths.root())) {
                try (Stream<Path> s = Files.list(paths.root())) {
                    s.filter(Files::isDirectory)
                            .filter(d -> !paths.isIgnored(d))
                            .filter(d -> d.getFileName().toString().equals(VaultPaths.INBOX)
                                    || Files.exists(paths.projectFile(d)))
                            .forEach(dirs::add);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan task folders", e);
        }
        return dirs;
    }

    private List<Path> taskFiles(Path dir) {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return files;
        }
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".md"))
                    .filter(f -> !f.getFileName().toString().equals(VaultPaths.PROJECT_FILE))
                    .forEach(files::add);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list tasks in " + dir, e);
        }
        return files;
    }
}
