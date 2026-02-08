package com.ovrtechnology.entity.sniffer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ovrtechnology.AromaAffect;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and manages the sniffer configuration.
 */
public final class SnifferConfigLoader {

    private static final String CONFIG_PATH = "data/aromaaffect/config/sniffer_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Getter
    private static SnifferConfig config = new SnifferConfig();

    private SnifferConfigLoader() {}

    /**
     * Initializes the config system by loading the configuration file.
     */
    public static void init() {
        loadConfig();
    }

    /**
     * Loads the configuration from the JSON file.
     * First tries to load from the game's config directory,
     * then falls back to the default config in resources.
     */
    public static void loadConfig() {
        // Try loading from external config first
        Path externalConfig = getExternalConfigPath();
        if (Files.exists(externalConfig)) {
            try {
                String json = Files.readString(externalConfig, StandardCharsets.UTF_8);
                config = GSON.fromJson(json, SnifferConfig.class);
                AromaAffect.LOGGER.info("Loaded sniffer config from external file: {}", externalConfig);
                return;
            } catch (IOException e) {
                AromaAffect.LOGGER.warn("Failed to load external sniffer config, using default", e);
            }
        }

        // Load default config from resources
        loadDefaultConfig();

        // Save default config to external location for user editing
        saveDefaultConfig(externalConfig);
    }

    /**
     * Loads the default configuration from mod resources.
     */
    private static void loadDefaultConfig() {
        try (InputStream is = SnifferConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_PATH)) {
            if (is != null) {
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    config = GSON.fromJson(reader, SnifferConfig.class);
                    AromaAffect.LOGGER.info("Loaded default sniffer config from resources");
                }
            } else {
                AromaAffect.LOGGER.warn("Default sniffer config not found in resources, using hardcoded defaults");
                config = new SnifferConfig();
            }
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to load default sniffer config", e);
            config = new SnifferConfig();
        }
    }

    /**
     * Saves the current config to an external file for user editing.
     */
    private static void saveDefaultConfig(Path path) {
        try {
            Files.createDirectories(path.getParent());
            String json = GSON.toJson(config);
            Files.writeString(path, json, StandardCharsets.UTF_8);
            AromaAffect.LOGGER.info("Saved default sniffer config to: {}", path);
        } catch (IOException e) {
            AromaAffect.LOGGER.warn("Failed to save default sniffer config to external file", e);
        }
    }

    /**
     * Gets the path to the external config file.
     */
    private static Path getExternalConfigPath() {
        return Path.of("config", AromaAffect.MOD_ID, "sniffer_config.json");
    }

    /**
     * Reloads the configuration from disk.
     */
    public static void reload() {
        loadConfig();
        AromaAffect.LOGGER.info("Sniffer config reloaded");
    }
}
