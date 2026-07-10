package com.mytaman.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a markdown body into named {@code ## Heading} sections and reassembles them.
 *
 * <p>Parallel in spirit to {@link Frontmatter}: just as unknown YAML keys survive a
 * round-trip, unknown sections (e.g. a {@code ## Notes} block a user adds in Obsidian) are
 * preserved in order. Recognized sections ({@link #DESCRIPTION}, {@link #PROGRESS}) map to
 * model fields; everything else is passed through untouched.
 *
 * <p>Section boundaries are {@code ## } (exactly two hashes) headings. Content within a
 * section may use deeper headings ({@code ###} and beyond) freely, but should avoid
 * {@code ##}-level headings since those start a new section.
 *
 * <p>Backward compatibility: a body with no {@code ## } heading is treated as a single
 * {@link #DESCRIPTION} section, so legacy heading-less files (where the whole body was the
 * description) keep reading correctly and gain explicit headings on the next write.
 */
public final class BodyParser {

    public static final String DESCRIPTION = "Description";
    public static final String PROGRESS = "Progress";

    /** Canonical heading order; any unrecognized sections are emitted after these. */
    private static final List<String> CANONICAL = List.of(DESCRIPTION, PROGRESS);

    // A top-level section heading: "## " followed by a title, but not "###" or deeper.
    private static final Pattern HEADING = Pattern.compile("^##(?!#)[ \\t]+(.+?)[ \\t]*$");

    private BodyParser() {
    }

    /**
     * Parse a body into an ordered {@code heading -> content} map (both trimmed). Blank
     * sections are omitted. Any text before the first heading is folded into
     * {@link #DESCRIPTION}.
     */
    public static Map<String, String> parse(String body) {
        Map<String, String> sections = new LinkedHashMap<>();
        if (body == null || body.isBlank()) {
            return sections;
        }
        String heading = null; // null => preamble (folded into Description)
        StringBuilder buf = new StringBuilder();
        for (String line : body.split("\n", -1)) {
            Matcher m = HEADING.matcher(line);
            if (m.matches()) {
                flush(sections, heading, buf);
                heading = m.group(1).strip();
                buf.setLength(0);
            } else {
                buf.append(line).append('\n');
            }
        }
        flush(sections, heading, buf);
        return sections;
    }

    private static void flush(Map<String, String> sections, String heading, StringBuilder buf) {
        String content = buf.toString().strip();
        if (content.isEmpty()) {
            return;
        }
        sections.put(heading == null ? DESCRIPTION : heading, content);
    }

    /**
     * Render sections back to a markdown body. Recognized headings come first in canonical
     * order, then any others in insertion order. Null/blank sections are skipped, so an
     * all-blank map serializes to an empty body.
     */
    public static String serialize(Map<String, String> sections) {
        List<String> blocks = new ArrayList<>();
        for (String key : CANONICAL) {
            appendBlock(blocks, key, sections.get(key));
        }
        for (Map.Entry<String, String> e : sections.entrySet()) {
            if (!CANONICAL.contains(e.getKey())) {
                appendBlock(blocks, e.getKey(), e.getValue());
            }
        }
        return String.join("\n\n", blocks);
    }

    private static void appendBlock(List<String> blocks, String heading, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        blocks.add("## " + heading + "\n\n" + content.strip());
    }
}
