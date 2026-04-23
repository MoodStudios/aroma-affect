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

public final class ClientConfig {

    private static final String CONFIG_FILE_NAME = "aromaaffect_client.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ClientConfig instance;

    @Getter @Setter private String puffMode = "automatic";

    @Getter @Setter private String manualPuffKey = "G";

    @Getter @Setter private double globalIntensityMultiplier = 1.0;

    @Getter @Setter private long globalCooldownMs = 3000;

    @Getter @Setter private boolean noseRenderEnabled = true;

    @Getter @Setter private boolean strapEnabled = false;

    @Getter @Setter private boolean trackingToastPersistent = false;

    @Getter @Setter private boolean passivePuffOverlay = true;

    @Getter @Setter private boolean debugScentMessages = false;

    @Getter @Setter private long passiveBlockCooldownMs = 5000;

    @Getter @Setter private long passiveMobCooldownMs = 8000;

    @Getter @Setter private long passivePassiveMobCooldownMs = 8000;

    @Getter @Setter private double passiveBlockRange = 2.5;

    @Getter @Setter private double passiveMobRange = 5.0;

    private ClientConfig() {}

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
                AromaAffect.LOGGER.warn(
                        "Failed to load client config, using defaults: {}", e.getMessage());
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
