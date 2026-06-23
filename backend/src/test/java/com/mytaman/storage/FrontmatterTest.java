package com.mytaman.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontmatterTest {

    @Test
    void parsesMetadataAndBody() {
        String content = """
                ---
                id: TASK-7
                name: Fix login bug
                state: in-progress
                due: 2026-07-01
                ---

                The login form throws a 500.
                """;
        Frontmatter fm = Frontmatter.parse(content);

        assertEquals("TASK-7", fm.metadata().get("id"));
        assertEquals("Fix login bug", fm.metadata().get("name"));
        assertEquals("in-progress", fm.metadata().get("state"));
        // Dates must remain plain strings, not parsed into Date objects.
        assertEquals("2026-07-01", fm.metadata().get("due"));
        assertEquals("The login form throws a 500.", fm.body().strip());
    }

    @Test
    void noFrontmatterTreatsAllAsBody() {
        Frontmatter fm = Frontmatter.parse("just a note\nwith lines");
        assertTrue(fm.metadata().isEmpty());
        assertEquals("just a note\nwith lines", fm.body().strip());
    }

    @Test
    void roundTripPreservesFieldsAndUnknownKeys() {
        String content = """
                ---
                id: TASK-1
                name: Sample
                state: new
                tags: [a, b]
                ---

                Body text here.
                """;
        Frontmatter parsed = Frontmatter.parse(content);
        String serialized = parsed.serialize();
        Frontmatter again = Frontmatter.parse(serialized);

        assertEquals("TASK-1", again.metadata().get("id"));
        assertEquals("Sample", again.metadata().get("name"));
        assertEquals("new", again.metadata().get("state"));
        // Unknown key added by the user (e.g. in Obsidian) survives the round-trip.
        assertTrue(again.metadata().containsKey("tags"));
        assertEquals("Body text here.", again.body().strip());
    }

    @Test
    void serializeProducesDelimitedBlock() {
        Frontmatter fm = Frontmatter.parse("""
                ---
                id: PROJ-3
                name: Alpha
                ---

                Background.
                """);
        String out = fm.serialize();
        assertTrue(out.startsWith("---\n"));
        assertTrue(out.contains("id: PROJ-3"));
        assertTrue(out.contains("Background."));
    }
}
