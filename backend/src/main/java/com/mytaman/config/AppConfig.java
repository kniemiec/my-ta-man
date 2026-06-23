package com.mytaman.config;

import java.nio.file.Path;

/**
 * Runtime configuration read from environment variables.
 * <ul>
 *   <li>{@code VAULT_DIR} — vault root (default {@code /vault}).</li>
 *   <li>{@code PORT} — HTTP listen port (default {@code 7000}).</li>
 * </ul>
 */
public record AppConfig(Path vaultDir, int port) {

    public static AppConfig fromEnv() {
        String vault = envOr("VAULT_DIR", "/vault");
        int port = Integer.parseInt(envOr("PORT", "7000"));
        return new AppConfig(Path.of(vault), port);
    }

    private static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
