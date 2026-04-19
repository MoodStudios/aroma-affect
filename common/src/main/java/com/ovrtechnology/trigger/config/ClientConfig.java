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

    /** Whether nose strap rendering is enabled */
    @Getter @Setter
    private boolean strapEnabled = false;

    /** Whether to show persistent tracking toast while actively tracking */
    @Getter @Setter
    private boolean trackingToastPersistent = false;

    /** Whether to show the scent border overlay when passive mode triggers a puff */
    @Getter @Setter
    private boolean passivePuffOverlay = true;

    /** Whether to show debug chat messages when scents are triggered */
    @Getter @Setter
    private boolean debugScentMessages = false;

    // Passive mode settings

    /** Cooldown for block triggers in passive mode (ms) */
    @Getter @Setter
    private long passiveBlockCooldownMs = 5000;

    /** Cooldown for hostile mob triggers in passive mode (ms) */
    @Getter @Setter
    private long passiveMobCooldownMs = 8000;

    /** Cooldown for non-hostile mob triggers in passive mode (ms) */
    @Getter @Setter
    private long passivePassiveMobCooldownMs = 8000;

    /** Activation range for block triggers in passive mode (blocks) */
    @Getter @Setter
    private double passiveBlockRange = 2.5;

    /** Activation range for mob triggers in passive mode (blocks) */
    @Getter @Setter
    private double passiveMobRange = 5.0;

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
        strapEnabled = false;
        trackingToastPersistent = false;
        passivePuffOverlay = true;
        debugScentMessages = false;
        resetPassiveDefaults();
    }

    public void resetPassiveDefaults() {
        passiveBlockCooldownMs = 5000;
        passiveMobCooldownMs = 8000;
        passivePassiveMobCooldownMs = 8000;
        passiveBlockRange = 2.5;
        passiveMobRange = 5.0;
        save();
    }

    private static Path getConfigPath() {
        return Platform.getConfigFolder().resolve(CONFIG_FILE_NAME);
    }
}
