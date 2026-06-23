package com.mytaman;

import com.mytaman.config.AppConfig;
import io.javalin.Javalin;

/** Entry point: read config from env, start the HTTP server. */
public final class Main {

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromEnv();
        Javalin app = App.create(config);
        app.start(config.port());
    }
}
