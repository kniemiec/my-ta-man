package com.mytaman.storage;

import com.mytaman.model.Project;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Persistence for projects. A project is a folder at the vault root containing a
 * {@code _project.md} metadata file; the folder location is the source of truth.
 */
public final class ProjectRepository {

    private final VaultPaths paths;

    public ProjectRepository(VaultPaths paths) {
        this.paths = paths;
    }

    /** All projects (folders with a {@code _project.md}), in id order is not guaranteed. */
    public List<Project> findAll() {
        List<Project> result = new ArrayList<>();
        try {
            if (!Files.isDirectory(paths.root())) {
                return result;
            }
            try (Stream<Path> dirs = Files.list(paths.root())) {
                dirs.filter(Files::isDirectory)
                        .filter(d -> !paths.isIgnored(d))
                        .filter(d -> !d.getFileName().toString().equals(VaultPaths.INBOX))
                        .filter(d -> Files.exists(paths.projectFile(d)))
                        .forEach(d -> result.add(readProject(d)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list projects in " + paths.root(), e);
        }
        return result;
    }

    public Optional<Project> findById(String id) {
        return dirForId(id).map(this::readProject);
    }

    /** The project folder for a given id, by scanning {@code _project.md} frontmatter. */
    public Optional<Path> dirForId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return findAllDirs().stream()
                .filter(d -> id.equals(idForDir(d)))
                .findFirst();
    }

    /** Read a project's id from its folder, or null if it is not a project folder. */
    public String idForDir(Path dir) {
        Path file = paths.projectFile(dir);
        if (!Files.exists(file)) {
            return null;
        }
        return EntityMapper.toProject(MarkdownStore.read(file)).getId();
    }

    public Project readProject(Path dir) {
        Project p = EntityMapper.toProject(MarkdownStore.read(paths.projectFile(dir)));
        p.setTaskCount(countTasks(dir));
        return p;
    }

    /** Create a new project folder + {@code _project.md} from a fully-populated frontmatter. */
    public Path create(String baseSlug, Frontmatter fm) {
        Path dir = paths.uniqueDir(baseSlug);
        MarkdownStore.write(paths.projectFile(dir), fm);
        return dir;
    }

    /** Delete the project folder (caller enforces the non-empty rule). */
    public void deleteDir(Path dir) {
        try {
            MarkdownStore.delete(paths.projectFile(dir));
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete project folder: " + dir, e);
        }
    }

    /** Count task files (every {@code .md} except {@code _project.md}) in a folder. */
    public int countTasks(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return (int) files
                    .filter(f -> f.getFileName().toString().endsWith(".md"))
                    .filter(f -> !f.getFileName().toString().equals(VaultPaths.PROJECT_FILE))
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to count tasks in " + dir, e);
        }
    }

    private List<Path> findAllDirs() {
        List<Path> dirs = new ArrayList<>();
        try {
            if (!Files.isDirectory(paths.root())) {
                return dirs;
            }
            try (Stream<Path> s = Files.list(paths.root())) {
                s.filter(Files::isDirectory)
                        .filter(d -> !paths.isIgnored(d))
                        .filter(d -> !d.getFileName().toString().equals(VaultPaths.INBOX))
                        .filter(d -> Files.exists(paths.projectFile(d)))
                        .forEach(dirs::add);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan project folders", e);
        }
        return dirs;
    }
}
