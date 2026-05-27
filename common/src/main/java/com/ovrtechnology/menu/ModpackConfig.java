package com.ovrtechnology.menu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ovrtechnology.AromaAffect;
import dev.architectury.platform.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;

public final class ModpackConfig {

    private static final String CONFIG_FILE_NAME = "aromaaffect_modpack.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ModpackConfig instance;

    @Getter @Setter private String modpackName = "";

    private ModpackConfig() {}

    public static ModpackConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static ModpackConfig load() {
        Path configPath = getConfigPath();

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                ModpackConfig config = GSON.fromJson(json, ModpackConfig.class);
                if (config != null) {
                    AromaAffect.LOGGER.info(
                            "Loaded modpack config (modpackName='{}')", config.modpackName);
                    return config;
                }
            } catch (IOException e) {
                AromaAffect.LOGGER.warn(
                        "Failed to load modpack config, using defaults: {}", e.getMessage());
            }
        }

        AromaAffect.LOGGER.info("Creating default modpack config");
        ModpackConfig config = new ModpackConfig();
        config.save();
        return config;
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(this));
            AromaAffect.LOGGER.debug("Saved modpack config");
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to save modpack config: {}", e.getMessage());
        }
    }

    private static Path getConfigPath() {
        return Platform.getConfigFolder().resolve(CONFIG_FILE_NAME);
    }
}
