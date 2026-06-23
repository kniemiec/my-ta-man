package com.mytaman.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Low-level file primitives for markdown notes: read/parse, atomic write, delete, move.
 * Stateless and reusable by both repositories.
 */
public final class MarkdownStore {

    private MarkdownStore() {
    }

    public static Frontmatter read(Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            return Frontmatter.parse(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read note: " + file, e);
        }
    }

    /** Write {@code fm} to {@code file} atomically (temp file + atomic rename). */
    public static void write(Path file, Frontmatter fm) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("." + file.getFileName() + ".tmp");
            Files.writeString(tmp, fm.serialize(), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write note: " + file, e);
        }
    }

    public static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete note: " + file, e);
        }
    }

    /** Move {@code file} into {@code targetDir}, keeping its filename. Returns the new path. */
    public static Path move(Path file, Path targetDir) {
        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(file.getFileName());
            return Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            try {
                return Files.move(file, targetDir.resolve(file.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to move note: " + file, ex);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to move note: " + file, e);
        }
    }
}
