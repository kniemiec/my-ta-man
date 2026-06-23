package com.mytaman.api;

import com.mytaman.service.CreateProjectRequest;
import com.mytaman.service.ProjectService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

/** REST endpoints for projects. */
public final class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    public void register(Javalin app) {
        app.get("/api/projects", this::list);
        app.post("/api/projects", this::create);
        app.get("/api/projects/{id}", this::get);
        app.patch("/api/projects/{id}", this::patch);
        app.delete("/api/projects/{id}", this::delete);
    }

    private void list(Context ctx) {
        ctx.json(service.list());
    }

    private void create(Context ctx) {
        CreateProjectRequest req = ctx.bodyAsClass(CreateProjectRequest.class);
        ctx.status(201).json(service.create(req));
    }

    private void get(Context ctx) {
        ctx.json(service.get(ctx.pathParam("id")));
    }

    private void patch(Context ctx) {
        ctx.json(service.patch(ctx.pathParam("id"), readChanges(ctx)));
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
