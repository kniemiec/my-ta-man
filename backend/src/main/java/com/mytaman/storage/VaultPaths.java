package com.mytaman.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Resolves locations inside the vault and turns human names into safe filename slugs.
 *
 * <p>Layout:
 * <pre>
 *   $vault/
 *     .mytaman/counters.json     hidden id counters
 *     &lt;ProjectFolder&gt;/
 *       _project.md              project metadata + description
 *       &lt;task-slug&gt;.md           one file per task
 *     Inbox/                     tasks with no project
 * </pre>
 */
public final class VaultPaths {

    public static final String INBOX = "Inbox";
    /** Logical project id used to address the Inbox via the API. */
    public static final String INBOX_ID = "inbox";
    public static final String PROJECT_FILE = "_project.md";
    private static final String META_DIR = ".mytaman";
    private static final String COUNTERS_FILE = "counters.json";

    private final Path root;

    public VaultPaths(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public Path countersFile() {
        return root.resolve(META_DIR).resolve(COUNTERS_FILE);
    }

    public Path inboxDir() {
        return root.resolve(INBOX);
    }

    /** The metadata file for a project folder. */
    public Path projectFile(Path projectDir) {
        return projectDir.resolve(PROJECT_FILE);
    }

    /** True for folders/files the scanner must ignore (hidden, Obsidian internals). */
    public boolean isIgnored(Path dir) {
        String name = dir.getFileName().toString();
        return name.startsWith(".");
    }

    /**
     * Turn a display name into a filesystem-safe slug (lowercase, hyphen-separated,
     * ascii). Falls back to {@code untitled} when nothing usable remains.
     */
    public static String slug(String name) {
        if (name == null) {
            return "untitled";
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isBlank() ? "untitled" : slug;
    }

    /**
     * Resolve a unique {@code <slug>.md} path within {@code dir}, appending {@code -2},
     * {@code -3}, … if a file already exists. Used so two tasks/projects with the same
     * name don't clobber each other.
     */
    public Path uniqueFile(Path dir, String baseSlug) {
        Path candidate = dir.resolve(baseSlug + ".md");
        int n = 2;
        while (Files.exists(candidate)) {
            candidate = dir.resolve(baseSlug + "-" + n + ".md");
            n++;
        }
        return candidate;
    }

    /**
     * Resolve a unique project folder path within the vault root, appending a numeric
     * suffix on collision.
     */
    public Path uniqueDir(String baseSlug) {
        Path candidate = root.resolve(baseSlug);
        int n = 2;
        while (Files.exists(candidate)) {
            candidate = root.resolve(baseSlug + "-" + n);
            n++;
        }
        return candidate;
    }
}
