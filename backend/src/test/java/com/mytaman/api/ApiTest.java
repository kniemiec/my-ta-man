package com.mytaman.api;

import com.mytaman.App;
import com.mytaman.config.AppConfig;
import com.mytaman.storage.Json;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTest {

    @TempDir
    Path vault;

    private Javalin app;
    private HttpClient http;
    private String base;

    @BeforeEach
    void setUp() {
        app = App.create(new AppConfig(vault, 0));
        app.start(0);
        base = "http://localhost:" + app.port();
        http = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        app.stop();
    }

    @Test
    void fullTaskLifecycleOverHttp() throws Exception {
        // Create project
        Response proj = post("/api/projects", Map.of("name", "Alpha", "description", "Why."));
        assertEquals(201, proj.status());
        String projectId = (String) proj.json().get("id");
        assertEquals("PROJ-1", projectId);

        // Create task in project
        Response task = post("/api/tasks", Map.of("name", "Fix bug", "projectId", projectId));
        assertEquals(201, task.status());
        String taskId = (String) task.json().get("id");

        // The file exists on disk and is a valid Obsidian note
        Path file = vault.resolve("alpha").resolve("fix-bug.md");
        assertTrue(Files.exists(file));
        assertTrue(Files.readString(file).contains("state: new"));

        // Patch state -> in-progress
        Response patched = patch("/api/tasks/" + taskId, Map.of("state", "in-progress"));
        assertEquals(200, patched.status());
        assertEquals("in-progress", patched.json().get("state"));
        assertTrue(Files.readString(file).contains("state: in-progress"));

        // List tasks by project
        Response list = get("/api/tasks?project=" + projectId);
        assertEquals(200, list.status());

        // Delete project while it has a task -> 409
        Response conflict = delete("/api/projects/" + projectId);
        assertEquals(409, conflict.status());

        // Delete task -> 204
        Response del = delete("/api/tasks/" + taskId);
        assertEquals(204, del.status());

        // Now project deletes cleanly
        assertEquals(204, delete("/api/projects/" + projectId).status());
    }

    @Test
    void unknownTaskReturns404() throws Exception {
        assertEquals(404, get("/api/tasks/TASK-999").status());
    }

    @Test
    void blankNameReturns400() throws Exception {
        assertEquals(400, post("/api/tasks", Map.of("name", "  ")).status());
    }

    // --- HTTP helpers ---

    private record Response(int status, Map<String, Object> json) {
    }

    private Response get(String path) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(base + path)).GET().build());
    }

    private Response post(String path, Map<String, Object> body) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.MAPPER.writeValueAsString(body)))
                .build());
    }

    private Response patch(String path, Map<String, Object> body) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(Json.MAPPER.writeValueAsString(body)))
                .build());
    }

    private Response delete(String path) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(base + path)).DELETE().build());
    }

    @SuppressWarnings("unchecked")
    private Response send(HttpRequest req) throws Exception {
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> json = Map.of();
        if (resp.body() != null && !resp.body().isBlank()) {
            Object parsed = Json.MAPPER.readValue(resp.body(), Object.class);
            if (parsed instanceof Map<?, ?> m) {
                json = (Map<String, Object>) m;
            }
        }
        return new Response(resp.statusCode(), json);
    }
}
