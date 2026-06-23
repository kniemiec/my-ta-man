package com.mytaman.storage;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Issues stable, monotonically increasing ids ({@code TASK-7}, {@code PROJ-3}).
 *
 * <p>Counters are persisted to {@code .mytaman/counters.json} so deleting the highest id
 * never causes reuse. An in-process {@link ReentrantLock} serializes increments; this is a
 * single-user local app so no cross-process coordination is required.
 */
public final class IdSequence {

    private static final String TASK_KEY = "task";
    private static final String PROJECT_KEY = "project";

    private final Path countersFile;
    private final ReentrantLock lock = new ReentrantLock();

    public IdSequence(Path countersFile) {
        this.countersFile = countersFile;
    }

    public String nextTaskId() {
        return "TASK-" + next(TASK_KEY);
    }

    public String nextProjectId() {
        return "PROJ-" + next(PROJECT_KEY);
    }

    private long next(String key) {
        lock.lock();
        try {
            Map<String, Long> counters = read();
            long value = counters.getOrDefault(key, 0L) + 1;
            counters.put(key, value);
            write(counters);
            return value;
        } finally {
            lock.unlock();
        }
    }

    private Map<String, Long> read() {
        try {
            if (!Files.exists(countersFile)) {
                return new LinkedHashMap<>();
            }
            byte[] bytes = Files.readAllBytes(countersFile);
            if (bytes.length == 0) {
                return new LinkedHashMap<>();
            }
            return Json.MAPPER.readValue(bytes, new TypeReference<LinkedHashMap<String, Long>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read id counters: " + countersFile, e);
        }
    }

    private void write(Map<String, Long> counters) {
        try {
            Files.createDirectories(countersFile.getParent());
            Path tmp = countersFile.resolveSibling(countersFile.getFileName() + ".tmp");
            Files.write(tmp, Json.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(counters));
            try {
                Files.move(tmp, countersFile,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, countersFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write id counters: " + countersFile, e);
        }
    }
}
