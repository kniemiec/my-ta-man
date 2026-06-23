package com.mytaman.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VaultPathsTest {

    @Test
    void slugifiesNames() {
        assertEquals("fix-login-bug", VaultPaths.slug("Fix Login Bug"));
        assertEquals("add-dark-mode", VaultPaths.slug("Add  dark/mode!"));
        assertEquals("cafe", VaultPaths.slug("Café"));
        assertEquals("untitled", VaultPaths.slug("   "));
        assertEquals("untitled", VaultPaths.slug(null));
    }

    @Test
    void uniqueFileAppendsSuffixOnCollision(@TempDir Path dir) throws IOException {
        VaultPaths paths = new VaultPaths(dir);
        Path first = paths.uniqueFile(dir, "task");
        assertEquals("task.md", first.getFileName().toString());

        Files.createFile(first);
        Path second = paths.uniqueFile(dir, "task");
        assertNotEquals(first, second);
        assertEquals("task-2.md", second.getFileName().toString());
    }

    @Test
    void uniqueDirAppendsSuffixOnCollision(@TempDir Path dir) throws IOException {
        VaultPaths paths = new VaultPaths(dir);
        Path first = paths.uniqueDir("alpha");
        Files.createDirectories(first);
        Path second = paths.uniqueDir("alpha");
        assertEquals("alpha-2", second.getFileName().toString());
    }
}
