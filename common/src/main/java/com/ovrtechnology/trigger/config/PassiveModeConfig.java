package com.ovrtechnology.trigger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ovrtechnology.AromaAffect;
import dev.architectury.platform.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;

public final class PassiveModeConfig {

    private static final String CONFIG_FILE_NAME = "aromaaffect_passive_mode.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static PassiveModeConfig instance;

    @Getter @Setter private boolean passiveModeEnabled = false;

    @Getter @Setter private Boolean eventEnabledBeforeDisable = null;

    private PassiveModeConfig() {}

    public static PassiveModeConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static PassiveModeConfig load() {
        Path configPath = getConfigPath();

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                PassiveModeConfig config = GSON.fromJson(json, PassiveModeConfig.class);
                if (config != null) {
                    AromaAffect.LOGGER.info(
                            "Loaded passive mode config: enabled={}", config.passiveModeEnabled);
                    return config;
                }
            } catch (IOException e) {
                AromaAffect.LOGGER.warn(
                        "Failed to load passive mode config, using defaults: {}", e.getMessage());
            }
        }

        AromaAffect.LOGGER.info("Creating default passive mode config");
        return new PassiveModeConfig();
    }

    public void save() {
        Path configPath = getConfigPath();

        try {

            Files.createDirectories(configPath.getParent());

            String json = GSON.toJson(this);
            Files.writeString(configPath, json);
            AromaAffect.LOGGER.debug("Saved passive mode config: enabled={}", passiveModeEnabled);
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to save passive mode config: {}", e.getMessage());
        }
    }

    private static Path getConfigPath() {
        return Platform.getConfigFolder().resolve(CONFIG_FILE_NAME);
    }
}
