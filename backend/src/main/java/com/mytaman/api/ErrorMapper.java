package com.mytaman.api;

import com.mytaman.service.ConflictException;
import com.mytaman.service.NotFoundException;
import com.mytaman.service.ValidationException;
import io.javalin.Javalin;

import java.util.Map;

/** Maps domain exceptions to JSON problem bodies with the right HTTP status. */
public final class ErrorMapper {

    private ErrorMapper() {
    }

    public static void register(Javalin app) {
        app.exception(ValidationException.class, (e, ctx) ->
                ctx.status(400).json(body(400, e.getMessage())));
        app.exception(NotFoundException.class, (e, ctx) ->
                ctx.status(404).json(body(404, e.getMessage())));
        app.exception(ConflictException.class, (e, ctx) ->
                ctx.status(409).json(body(409, e.getMessage())));
        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(400).json(body(400, e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(body(500, "Internal error: " + e.getMessage()));
        });
    }

    private static Map<String, Object> body(int status, String message) {
        return Map.of("status", status, "error", message == null ? "" : message);
    }
}
