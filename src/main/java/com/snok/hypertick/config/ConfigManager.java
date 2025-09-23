package com.snok.hypertick.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

/**
 * Loads and saves a very small JSON config at config/hyper-tick.json.
 * This keeps things simple and easy to tweak for beginners.
 */
public final class ConfigManager {
    public static final String FILE_NAME = "config/hyper-tick.json";

    public static final class Config {
        public String mode = "FIRST"; // FIRST or LAST (future)
        public int buffer_rate = 1000; // Hz
        public int[] priority_slots = new int[0];
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {}

    public static Config loadOrCreateDefault() {
        try {
            File f = new File(FILE_NAME);
            if (!f.exists()) {
                Config cfg = new Config();
                save(cfg);
                return cfg;
            }
            try (BufferedReader br = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
                return GSON.fromJson(br, Config.class);
            }
        } catch (Exception e) {
            // If anything goes wrong, return defaults so the mod still runs
            Config cfg = new Config();
            try { save(cfg); } catch (Exception ignored) {}
            return cfg;
        }
    }

    public static void save(Config cfg) throws Exception {
        File f = new File(FILE_NAME);
        f.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, StandardCharsets.UTF_8))) {
            bw.write(GSON.toJson(cfg));
        }
    }

    public static File getConfigFile() {
        return new File(FILE_NAME);
    }
}


