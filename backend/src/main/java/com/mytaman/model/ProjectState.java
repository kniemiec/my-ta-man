package com.mytaman.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Project lifecycle states, serialized as kebab-case in JSON and frontmatter.
 */
public enum ProjectState {
    NEW("new"),
    IN_PROGRESS("in-progress"),
    COMPLETED("completed");

    private final String wire;

    ProjectState(String wire) {
        this.wire = wire;
    }

    @JsonValue
    public String wire() {
        return wire;
    }

    @JsonCreator
    public static ProjectState fromWire(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return Arrays.stream(values())
                .filter(s -> s.wire.equalsIgnoreCase(v))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown project state: " + value));
    }
}
