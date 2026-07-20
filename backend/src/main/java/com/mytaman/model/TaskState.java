package com.mytaman.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Task lifecycle states. Serialized to/from kebab-case (e.g. {@code in-progress})
 * in both JSON and the markdown frontmatter so files stay human-friendly.
 */
public enum TaskState {
    NEW("new"),
    IN_PROGRESS("in-progress"),
    BLOCKED("blocked"),
    IN_REVIEW("in-review"),
    COMPLETED("completed"),
    ARCHIVED("archived");

    private final String wire;

    TaskState(String wire) {
        this.wire = wire;
    }

    @JsonValue
    public String wire() {
        return wire;
    }

    @JsonCreator
    public static TaskState fromWire(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return Arrays.stream(values())
                .filter(s -> s.wire.equalsIgnoreCase(v))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown task state: " + value));
    }
}
