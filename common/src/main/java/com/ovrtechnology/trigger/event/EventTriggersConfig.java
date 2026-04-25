package com.ovrtechnology.trigger.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.AromaAffect;
import dev.architectury.platform.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public final class EventTriggersConfig {

    private static final String CONFIG_FILE_NAME = "aromaaffect_event_triggers.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String CATEGORY_WEATHER = "WEATHER";
    public static final String CATEGORY_PLAYER_STATE = "PLAYER_STATE";
    public static final String CATEGORY_PLAYER_PROGRESS = "PLAYER_PROGRESS";
    public static final String CATEGORY_PLAYER_ACTION = "PLAYER_ACTION";
    public static final String CATEGORY_COMBAT = "COMBAT";
    public static final String CATEGORY_CRAFT_INTERACT = "CRAFT_INTERACT";
    public static final String CATEGORY_DIMENSION = "DIMENSION";
    public static final String CATEGORY_BLOCK_BREAK = "BLOCK_BREAK";

    private static EventTriggersConfig instance;

    @Getter @Setter
    @SerializedName("event_triggers_enabled")
    private boolean eventTriggersEnabled = true;

    @Getter @Setter
    @SerializedName("global_throttle_per_minute")
    private int globalThrottlePerMinute = 30;

    @Getter
    @SerializedName("category_enabled")
    private Map<String, Boolean> categoryEnabled = defaultCategoryEnabled();

    @Getter
    @SerializedName("category_cooldown_ms")
    private Map<String, Long> categoryCooldownMs = defaultCategoryCooldowns();

    private EventTriggersConfig() {}

    private static Map<String, Boolean> defaultCategoryEnabled() {
        Map<String, Boolean> defaults = new HashMap<>();
        defaults.put(CATEGORY_WEATHER, true);
        defaults.put(CATEGORY_PLAYER_STATE, true);
        defaults.put(CATEGORY_PLAYER_PROGRESS, true);
        defaults.put(CATEGORY_PLAYER_ACTION, true);
        defaults.put(CATEGORY_COMBAT, true);
        defaults.put(CATEGORY_CRAFT_INTERACT, true);
        defaults.put(CATEGORY_DIMENSION, true);
        defaults.put(CATEGORY_BLOCK_BREAK, true);
        return defaults;
    }

    private static Map<String, Long> defaultCategoryCooldowns() {
        Map<String, Long> defaults = new HashMap<>();
        defaults.put(CATEGORY_WEATHER, 15000L);
        defaults.put(CATEGORY_PLAYER_STATE, 5000L);
        defaults.put(CATEGORY_PLAYER_PROGRESS, 10000L);
        defaults.put(CATEGORY_PLAYER_ACTION, 5000L);
        defaults.put(CATEGORY_COMBAT, 5000L);
        defaults.put(CATEGORY_CRAFT_INTERACT, 8000L);
        defaults.put(CATEGORY_DIMENSION, 30000L);
        defaults.put(CATEGORY_BLOCK_BREAK, 3000L);
        return defaults;
    }

    public static EventTriggersConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static EventTriggersConfig load() {
        Path configPath = getConfigPath();

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                EventTriggersConfig config = GSON.fromJson(json, EventTriggersConfig.class);
                if (config != null) {
                    config.fillMissingDefaults();
                    AromaAffect.LOGGER.info(
                            "Loaded event triggers config: enabled={}, throttle={}/min",
                            config.eventTriggersEnabled,
                            config.globalThrottlePerMinute);
                    return config;
                }
            } catch (IOException e) {
                AromaAffect.LOGGER.warn(
                        "Failed to load event triggers config, using defaults: {}",
                        e.getMessage());
            }
        }

        AromaAffect.LOGGER.info("Creating default event triggers config");
        return new EventTriggersConfig();
    }

    private void fillMissingDefaults() {
        if (categoryEnabled == null) {
            categoryEnabled = defaultCategoryEnabled();
        } else {
            for (Map.Entry<String, Boolean> entry : defaultCategoryEnabled().entrySet()) {
                categoryEnabled.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        if (categoryCooldownMs == null) {
            categoryCooldownMs = defaultCategoryCooldowns();
        } else {
            for (Map.Entry<String, Long> entry : defaultCategoryCooldowns().entrySet()) {
                categoryCooldownMs.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        if (globalThrottlePerMinute <= 0) {
            globalThrottlePerMinute = 30;
        }
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            String json = GSON.toJson(this);
            Files.writeString(configPath, json);
            AromaAffect.LOGGER.debug(
                    "Saved event triggers config: enabled={}", eventTriggersEnabled);
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to save event triggers config: {}", e.getMessage());
        }
    }

    public boolean isCategoryEnabled(String category) {
        if (!eventTriggersEnabled) {
            return false;
        }
        return categoryEnabled.getOrDefault(category, true);
    }

    public void setCategoryEnabled(String category, boolean enabled) {
        categoryEnabled.put(category, enabled);
    }

    public long getCategoryCooldownMs(String category) {
        return categoryCooldownMs.getOrDefault(category, 5000L);
    }

    public void setCategoryCooldownMs(String category, long cooldownMs) {
        categoryCooldownMs.put(category, Math.max(0L, cooldownMs));
    }

    private static Path getConfigPath() {
        return Platform.getConfigFolder().resolve(CONFIG_FILE_NAME);
    }
}
