package com.mytaman.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdSequenceTest {

    @Test
    void issuesMonotonicTaskAndProjectIds(@TempDir Path dir) {
        IdSequence ids = new IdSequence(dir.resolve(".mytaman/counters.json"));
        assertEquals("TASK-1", ids.nextTaskId());
        assertEquals("TASK-2", ids.nextTaskId());
        assertEquals("PROJ-1", ids.nextProjectId());
        assertEquals("TASK-3", ids.nextTaskId());
        assertEquals("PROJ-2", ids.nextProjectId());
    }

    @Test
    void persistsAcrossInstancesSoIdsAreNotReused(@TempDir Path dir) {
        Path counters = dir.resolve(".mytaman/counters.json");
        IdSequence first = new IdSequence(counters);
        first.nextTaskId();
        first.nextTaskId();

        // A fresh instance (e.g. after restart) must continue, not reset.
        IdSequence second = new IdSequence(counters);
        assertEquals("TASK-3", second.nextTaskId());
    }
}
