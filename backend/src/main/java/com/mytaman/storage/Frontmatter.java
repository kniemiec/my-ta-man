package com.mytaman.storage;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and serializes an Obsidian-style note: an optional YAML frontmatter block
 * delimited by {@code ---} lines, followed by a free-form markdown body.
 *
 * <p>Dates such as {@code 2026-07-01} are kept as plain strings (not parsed into
 * {@link java.util.Date}) so they round-trip byte-for-byte and stay valid Obsidian
 * properties. Key order is preserved via a {@link LinkedHashMap}.
 */
public final class Frontmatter {

    // Matches a leading frontmatter block: --- ... --- at the very start of the file.
    private static final Pattern BLOCK = Pattern.compile(
            "\\A---\\s*\\R(.*?)\\R---[ \\t]*(?:\\R(.*)|\\z)",
            Pattern.DOTALL);

    private final Map<String, Object> metadata;
    private final String body;

    public Frontmatter(Map<String, Object> metadata, String body) {
        this.metadata = metadata != null ? metadata : new LinkedHashMap<>();
        this.body = body != null ? body : "";
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public String body() {
        return body;
    }

    /** Parse raw file content into frontmatter + body. Missing frontmatter yields an empty map. */
    public static Frontmatter parse(String content) {
        if (content == null) {
            return new Frontmatter(new LinkedHashMap<>(), "");
        }
        Matcher m = BLOCK.matcher(content);
        if (m.find()) {
            String yaml = m.group(1);
            String body = m.group(2) != null ? m.group(2) : "";
            Map<String, Object> meta = loadYaml(yaml);
            return new Frontmatter(meta, body);
        }
        // No frontmatter: the whole thing is body.
        return new Frontmatter(new LinkedHashMap<>(), content);
    }

    /** Serialize back to {@code ---\n<yaml>---\n\n<body>}. */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        if (!metadata.isEmpty()) {
            sb.append(dumpYaml(metadata));
        }
        sb.append("---\n");
        if (!body.isEmpty()) {
            sb.append("\n");
            sb.append(body);
            if (!body.endsWith("\n")) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object loaded = newYaml().load(yaml);
        if (loaded instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        return new LinkedHashMap<>();
    }

    private static String dumpYaml(Map<String, Object> metadata) {
        return newYaml().dump(metadata);
    }

    /**
     * A SnakeYAML instance with block style, no document markers, and timestamp
     * resolution disabled so date-like strings survive round-trips unchanged.
     */
    private static Yaml newYaml() {
        DumperOptions dumper = new DumperOptions();
        dumper.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumper.setSplitLines(false);

        LoaderOptions loader = new LoaderOptions();
        Representer representer = new Representer(dumper);
        return new Yaml(new SafeConstructor(loader), representer, dumper, loader, new NoTimestampResolver());
    }

    /** Removes the implicit timestamp resolver so ISO dates remain plain strings. */
    private static final class NoTimestampResolver extends Resolver {
        @Override
        protected void addImplicitResolvers() {
            addImplicitResolver(Tag.BOOL, BOOL, "yYnNtTfFoO");
            addImplicitResolver(Tag.INT, INT, "-+0123456789");
            addImplicitResolver(Tag.FLOAT, FLOAT, "-+0123456789.");
            addImplicitResolver(Tag.MERGE, MERGE, "<");
            addImplicitResolver(Tag.NULL, NULL, "~nN\0");
            addImplicitResolver(Tag.NULL, EMPTY, null);
            // Intentionally omit TIMESTAMP so dates stay as strings.
        }
    }
}
