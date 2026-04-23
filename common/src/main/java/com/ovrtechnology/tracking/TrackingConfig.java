package com.ovrtechnology.tracking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.AromaAffect;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TrackingConfig {

    private static final String CONFIG_FILE_NAME = "aromaaffect_tracking.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static TrackingConfig instance;

    @SerializedName("history_retrack_cost")
    private int historyRetrackCost = 3;

    public int getHistoryRetrackCost() {
        return historyRetrackCost > 0 ? historyRetrackCost : 3;
    }

    public static TrackingConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static TrackingConfig load() {
        Path configDir = getConfigDir();
        if (configDir == null) {
            AromaAffect.LOGGER.warn("Config directory not available, using default TrackingConfig");
            return new TrackingConfig();
        }

        Path configFile = configDir.resolve(CONFIG_FILE_NAME);

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                TrackingConfig config = GSON.fromJson(reader, TrackingConfig.class);
                if (config != null) {
                    AromaAffect.LOGGER.info(
                            "Loaded TrackingConfig: history_retrack_cost={}",
                            config.getHistoryRetrackCost());
                    return config;
                }
            } catch (Exception e) {
                AromaAffect.LOGGER.error("Failed to load {}, using defaults", CONFIG_FILE_NAME, e);
            }
        }

        TrackingConfig defaults = new TrackingConfig();
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(defaults, writer);
            }
            AromaAffect.LOGGER.info("Created default {}", CONFIG_FILE_NAME);
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
        }

        return defaults;
    }

    private static Path getConfigDir() {
        try {

            Path gameDir = Path.of(System.getProperty("user.dir", "."));
            Path configDir = gameDir.resolve("config");
            return configDir;
        } catch (Exception e) {
            return null;
        }
    }

    public static void reload() {
        instance = null;
        getInstance();
    }
}
