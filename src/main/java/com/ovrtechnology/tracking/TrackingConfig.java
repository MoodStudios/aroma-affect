package com.ovrtechnology.tracking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.util.ConfigPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for tracking costs.
 * Loads from aromaaffect_tracking.json in the game's config folder.
 * Creates the file with defaults if it doesn't exist.
 */
public class TrackingConfig {

    private static final String CONFIG_FILE_NAME = "aromaaffect_tracking.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static TrackingConfig instance;

    @SerializedName("history_retrack_cost")
    private int historyRetrackCost = 3;

    public TrackingConfig() {
    }

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
        Path configFile = ConfigPaths.resolve(CONFIG_FILE_NAME);

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                TrackingConfig config = GSON.fromJson(reader, TrackingConfig.class);
                if (config != null) {
                    AromaAffect.LOGGER.info("Loaded TrackingConfig: history_retrack_cost={}", config.getHistoryRetrackCost());
                    return config;
                }
            } catch (Exception e) {
                AromaAffect.LOGGER.error("Failed to load {}, using defaults", CONFIG_FILE_NAME, e);
            }
        }

        // Create default config file
        TrackingConfig defaults = new TrackingConfig();
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(defaults, writer);
            }
            AromaAffect.LOGGER.info("Created default {}", CONFIG_FILE_NAME);
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
        }

        return defaults;
    }

    /**
     * Force reload of the config (e.g., after manual edit).
     */
    public static void reload() {
        instance = null;
        getInstance();
    }
}
