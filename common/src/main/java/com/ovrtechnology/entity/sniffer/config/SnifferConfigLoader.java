package com.ovrtechnology.entity.sniffer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import lombok.Getter;

import java.io.IOException;
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
        loadConfig(ClasspathDataSource.INSTANCE);
    }

    public static void init(DataSource dataSource) {
        loadConfig(dataSource);
    }

    public static void loadConfig() {
        loadConfig(ClasspathDataSource.INSTANCE);
    }

    /**
     * Loads the configuration from the JSON file.
     * First tries the game's config directory, then the given {@link DataSource}
     * (classpath by default, or the live {@code ResourceManager} during reload),
     * finally hardcoded defaults.
     */
    public static void loadConfig(DataSource dataSource) {
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

        loadDefaultConfig(dataSource);
        saveDefaultConfig(externalConfig);
    }

    private static void loadDefaultConfig(DataSource dataSource) {
        JsonElement element = dataSource.read(CONFIG_PATH);
        if (element == null) {
            AromaAffect.LOGGER.warn("Default sniffer config not found at {}, using hardcoded defaults", CONFIG_PATH);
            config = new SnifferConfig();
            return;
        }
        try {
            config = GSON.fromJson(element, SnifferConfig.class);
            AromaAffect.LOGGER.info("Loaded default sniffer config");
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to parse default sniffer config: {}", e.getMessage());
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
        reload(ClasspathDataSource.INSTANCE);
    }

    public static void reload(DataSource dataSource) {
        loadConfig(dataSource);
        AromaAffect.LOGGER.info("Sniffer config reloaded");
    }
}
