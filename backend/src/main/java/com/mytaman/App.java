package com.mytaman;

import com.mytaman.api.ErrorMapper;
import com.mytaman.api.ProjectController;
import com.mytaman.api.TaskController;
import com.mytaman.config.AppConfig;
import com.mytaman.service.ProjectService;
import com.mytaman.service.TaskService;
import com.mytaman.storage.IdSequence;
import com.mytaman.storage.Json;
import com.mytaman.storage.ProjectRepository;
import com.mytaman.storage.TaskRepository;
import com.mytaman.storage.VaultPaths;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

/**
 * Composition root: wires storage, services, and controllers onto a configured (but not
 * yet started) Javalin instance. Kept separate from {@link Main} so tests can boot the
 * full app on an ephemeral port against a temp vault.
 */
public final class App {

    private App() {
    }

    public static Javalin create(AppConfig config) {
        VaultPaths paths = new VaultPaths(config.vaultDir());
        IdSequence ids = new IdSequence(paths.countersFile());
        ProjectRepository projectRepo = new ProjectRepository(paths);
        TaskRepository taskRepo = new TaskRepository(paths, projectRepo);

        TaskService taskService = new TaskService(taskRepo, ids);
        ProjectService projectService = new ProjectService(projectRepo, taskRepo, paths, ids);

        Javalin app = Javalin.create(cfg ->
                cfg.jsonMapper(new JavalinJackson(Json.MAPPER, false)));

        app.get("/api/health", ctx -> ctx.json(java.util.Map.of("status", "ok")));
        new ProjectController(projectService).register(app);
        new TaskController(taskService).register(app);
        ErrorMapper.register(app);

        return app;
    }
}
