package com.ovrtechnology.trigger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ovrtechnology.AromaAffect;
import dev.architectury.platform.Platform;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent client configuration for Aroma Affect settings.
 * Saves and loads from aromaaffect_client.json in the config directory.
 */
public final class ClientConfig {

    private static final String CONFIG_FILE_NAME = "aromaaffect_client.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ClientConfig instance;

    /** "automatic" or "manual" */
    @Getter @Setter
    private String puffMode = "automatic";

    /** GLFW key name for manual puff */
    @Getter @Setter
    private String manualPuffKey = "G";

    /** Global intensity multiplier 0.0-1.0 */
    @Getter @Setter
    private double globalIntensityMultiplier = 1.0;

    /** Global cooldown in milliseconds */
    @Getter @Setter
    private long globalCooldownMs = 3000;

    /** Whether 3D nose rendering is enabled */
    @Getter @Setter
    private boolean noseRenderEnabled = true;

    private ClientConfig() {
    }

    public static ClientConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static ClientConfig load() {
        Path configPath = getConfigPath();

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                ClientConfig config = GSON.fromJson(json, ClientConfig.class);
                if (config != null) {
                    AromaAffect.LOGGER.info("Loaded client config");
                    return config;
                }
            } catch (IOException e) {
                AromaAffect.LOGGER.warn("Failed to load client config, using defaults: {}", e.getMessage());
            }
        }

        AromaAffect.LOGGER.info("Creating default client config");
        ClientConfig config = new ClientConfig();
        config.save();
        return config;
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(this));
            AromaAffect.LOGGER.debug("Saved client config");
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to save client config: {}", e.getMessage());
        }
    }

    public void resetDefaults() {
        puffMode = "automatic";
        manualPuffKey = "G";
        globalIntensityMultiplier = 1.0;
        globalCooldownMs = 3000;
        noseRenderEnabled = true;
        save();
    }

    private static Path getConfigPath() {
        return Platform.getConfigFolder().resolve(CONFIG_FILE_NAME);
    }
}
