package com.mytaman.api;

import com.mytaman.service.CreateTaskRequest;
import com.mytaman.service.TaskService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

/** REST endpoints for tasks. */
public final class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    public void register(Javalin app) {
        app.get("/api/tasks", this::list);
        app.post("/api/tasks", this::create);
        app.get("/api/tasks/{id}", this::get);
        app.patch("/api/tasks/{id}", this::patch);
        app.patch("/api/tasks/{id}/move", this::move);
        app.delete("/api/tasks/{id}", this::delete);
    }

    private void list(Context ctx) {
        ctx.json(service.list(ctx.queryParam("project")));
    }

    private void create(Context ctx) {
        CreateTaskRequest req = ctx.bodyAsClass(CreateTaskRequest.class);
        ctx.status(201).json(service.create(req));
    }

    private void get(Context ctx) {
        ctx.json(service.get(ctx.pathParam("id")));
    }

    private void patch(Context ctx) {
        ctx.json(service.patch(ctx.pathParam("id"), readChanges(ctx)));
    }

    private void move(Context ctx) {
        Map<String, Object> body = readChanges(ctx);
        Object target = body.containsKey("projectId") ? body.get("projectId") : body.get("project");
        String targetId = target == null ? null : String.valueOf(target);
        ctx.json(service.move(ctx.pathParam("id"), targetId));
    }

    private void delete(Context ctx) {
        service.delete(ctx.pathParam("id"));
        ctx.status(204);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readChanges(Context ctx) {
        if (ctx.body().isBlank()) {
            return new LinkedHashMap<>();
        }
        return ctx.bodyAsClass(LinkedHashMap.class);
    }
}
