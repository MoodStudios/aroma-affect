package com.ovrtechnology.entity.sniffer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;

public final class SnifferConfigLoader {

    private static final String CONFIG_PATH = "data/aromaaffect/config/sniffer_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Getter private static SnifferConfig config = new SnifferConfig();

    private SnifferConfigLoader() {}

    public static void init() {
        loadConfig(ClasspathDataSource.INSTANCE);
    }

    public static void init(DataSource dataSource) {
        loadConfig(dataSource);
    }

    public static void loadConfig() {
        loadConfig(ClasspathDataSource.INSTANCE);
    }

    public static void loadConfig(DataSource dataSource) {
        Path externalConfig = getExternalConfigPath();
        if (Files.exists(externalConfig)) {
            try {
                String json = Files.readString(externalConfig, StandardCharsets.UTF_8);
                config = GSON.fromJson(json, SnifferConfig.class);
                AromaAffect.LOGGER.info(
                        "Loaded sniffer config from external file: {}", externalConfig);
                return;
            } catch (Exception e) {
                AromaAffect.LOGGER.warn(
                        "Failed to parse external sniffer config (likely legacy schema); backing up and regenerating defaults :)",
                        e);
                backupStaleConfig(externalConfig);
            }
        }

        loadDefaultConfig(dataSource);
        saveDefaultConfig(externalConfig);
    }

    private static void backupStaleConfig(Path externalConfig) {
        try {
            Path backup = externalConfig.resolveSibling("sniffer_config.json.bak");
            Files.move(
                    externalConfig,
                    backup,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            AromaAffect.LOGGER.warn("Stale sniffer config backed up to: {}", backup);
        } catch (IOException ioErr) {
            AromaAffect.LOGGER.error(
                    "Could not back up stale sniffer config at {}; attempting to delete instead",
                    externalConfig,
                    ioErr);
            try {
                Files.deleteIfExists(externalConfig);
            } catch (IOException deleteErr) {
                AromaAffect.LOGGER.error(
                        "Failed to delete stale sniffer config; please remove it manually: {}",
                        externalConfig,
                        deleteErr);
            }
        }
    }

    private static void loadDefaultConfig(DataSource dataSource) {
        JsonElement element = dataSource.read(CONFIG_PATH);
        if (element == null) {
            AromaAffect.LOGGER.warn(
                    "Default sniffer config not found at {}, using hardcoded defaults",
                    CONFIG_PATH);
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

    private static Path getExternalConfigPath() {
        return Path.of("config", AromaAffect.MOD_ID, "sniffer_config.json");
    }

    public static void reload() {
        reload(ClasspathDataSource.INSTANCE);
    }

    public static void reload(DataSource dataSource) {
        loadConfig(dataSource);
        AromaAffect.LOGGER.info("Sniffer config reloaded");
    }
}
