package com.mytaman.storage;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyParserTest {

    @Test
    void headinglessBodyIsTreatedAsDescription() {
        Map<String, String> sections = BodyParser.parse("Just a free-form note.\nSecond line.");
        assertEquals("Just a free-form note.\nSecond line.", sections.get(BodyParser.DESCRIPTION));
        assertFalse(sections.containsKey(BodyParser.PROGRESS));
    }

    @Test
    void parsesNamedSections() {
        String body = """
                ## Description

                The login form throws a 500.

                ## Progress

                - [x] Reproduced
                - [ ] Root cause found
                """;
        Map<String, String> sections = BodyParser.parse(body);
        assertEquals("The login form throws a 500.", sections.get(BodyParser.DESCRIPTION));
        assertEquals("- [x] Reproduced\n- [ ] Root cause found", sections.get(BodyParser.PROGRESS));
    }

    @Test
    void preambleBeforeFirstHeadingFoldsIntoDescription() {
        String body = """
                Loose intro text.

                ## Progress

                Started.
                """;
        Map<String, String> sections = BodyParser.parse(body);
        assertEquals("Loose intro text.", sections.get(BodyParser.DESCRIPTION));
        assertEquals("Started.", sections.get(BodyParser.PROGRESS));
    }

    @Test
    void roundTripPreservesContent() {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put(BodyParser.DESCRIPTION, "Some description.");
        sections.put(BodyParser.PROGRESS, "Halfway there.");
        String body = BodyParser.serialize(sections);

        Map<String, String> again = BodyParser.parse(body);
        assertEquals("Some description.", again.get(BodyParser.DESCRIPTION));
        assertEquals("Halfway there.", again.get(BodyParser.PROGRESS));
    }

    @Test
    void unknownSectionSurvivesRoundTrip() {
        String body = """
                ## Description

                Desc.

                ## Notes

                A user-added Obsidian section.
                """;
        Map<String, String> sections = BodyParser.parse(body);
        assertEquals("A user-added Obsidian section.", sections.get("Notes"));

        // Re-serialize and confirm the unknown section is retained, after the known ones.
        String out = BodyParser.serialize(sections);
        assertTrue(out.contains("## Notes"));
        assertTrue(out.contains("A user-added Obsidian section."));
    }

    @Test
    void knownSectionsEmittedInCanonicalOrder() {
        Map<String, String> sections = new LinkedHashMap<>();
        // Insert out of canonical order; serialize should still put Description first.
        sections.put(BodyParser.PROGRESS, "prog");
        sections.put(BodyParser.DESCRIPTION, "desc");
        String out = BodyParser.serialize(sections);
        assertTrue(out.indexOf("## Description") < out.indexOf("## Progress"));
    }

    @Test
    void blankSectionsAreOmitted() {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put(BodyParser.DESCRIPTION, "desc");
        sections.put(BodyParser.PROGRESS, "");
        String out = BodyParser.serialize(sections);
        assertTrue(out.contains("## Description"));
        assertFalse(out.contains("## Progress"));
    }

    @Test
    void allBlankSerializesToEmpty() {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put(BodyParser.DESCRIPTION, null);
        sections.put(BodyParser.PROGRESS, "   ");
        assertEquals("", BodyParser.serialize(sections));
    }

    @Test
    void deeperHeadingsInsideContentAreNotSectionBoundaries() {
        String body = """
                ## Description

                Intro.

                ### Sub heading

                Detail.
                """;
        Map<String, String> sections = BodyParser.parse(body);
        assertEquals(1, sections.size());
        assertTrue(sections.get(BodyParser.DESCRIPTION).contains("### Sub heading"));
    }
}
