package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TriggerSettings {

    public static final long DEFAULT_GLOBAL_COOLDOWN_MS = 3000;

    public static final long DEFAULT_SCENT_COOLDOWN_MS = 5000;

    public static final long DEFAULT_ITEM_USE_COOLDOWN_MS = 5000;

    public static final long DEFAULT_BIOME_COOLDOWN_MS = 15000;

    public static final long DEFAULT_BLOCK_COOLDOWN_MS = 5000;

    public static final long DEFAULT_MOB_COOLDOWN_MS = 8000;

    public static final long DEFAULT_STRUCTURE_COOLDOWN_MS = 30000;

    public static final long DEFAULT_PASSIVE_MOB_COOLDOWN_MS = 8000;

    public static final long DEFAULT_PATH_TRACKING_COOLDOWN_MS = 20000;

    public static final double DEFAULT_ITEM_INTENSITY = 0.5;

    public static final double DEFAULT_BIOME_INTENSITY = 0.3;

    public static final double DEFAULT_BLOCK_INTENSITY = 0.4;

    public static final double DEFAULT_STRUCTURE_INTENSITY = 0.35;

    public static final double DEFAULT_MOB_INTENSITY = 0.4;

    @SerializedName("global_cooldown_ms")
    private long globalCooldownMs = DEFAULT_GLOBAL_COOLDOWN_MS;

    @SerializedName("scent_cooldown_ms")
    private long scentCooldownMs = DEFAULT_SCENT_COOLDOWN_MS;

    @SerializedName("item_use_cooldown_ms")
    private long itemUseCooldownMs = DEFAULT_ITEM_USE_COOLDOWN_MS;

    @SerializedName("biome_cooldown_ms")
    private long biomeCooldownMs = DEFAULT_BIOME_COOLDOWN_MS;

    @SerializedName("block_cooldown_ms")
    private long blockCooldownMs = DEFAULT_BLOCK_COOLDOWN_MS;

    @SerializedName("mob_cooldown_ms")
    private long mobCooldownMs = DEFAULT_MOB_COOLDOWN_MS;

    @SerializedName("passive_mob_cooldown_ms")
    private long passiveMobCooldownMs = DEFAULT_PASSIVE_MOB_COOLDOWN_MS;

    @SerializedName("structure_cooldown_ms")
    private long structureCooldownMs = DEFAULT_STRUCTURE_COOLDOWN_MS;

    @SerializedName("path_tracking_cooldown_ms")
    private long pathTrackingCooldownMs = DEFAULT_PATH_TRACKING_COOLDOWN_MS;

    @SerializedName("item_intensity")
    private double itemIntensity = DEFAULT_ITEM_INTENSITY;

    @SerializedName("biome_intensity")
    private double biomeIntensity = DEFAULT_BIOME_INTENSITY;

    @SerializedName("block_intensity")
    private double blockIntensity = DEFAULT_BLOCK_INTENSITY;

    @SerializedName("structure_intensity")
    private double structureIntensity = DEFAULT_STRUCTURE_INTENSITY;

    @SerializedName("mob_intensity")
    private double mobIntensity = DEFAULT_MOB_INTENSITY;

    public void validate() {

        if (globalCooldownMs < 0) globalCooldownMs = DEFAULT_GLOBAL_COOLDOWN_MS;
        if (scentCooldownMs < 0) scentCooldownMs = DEFAULT_SCENT_COOLDOWN_MS;
        if (itemUseCooldownMs < 0) itemUseCooldownMs = DEFAULT_ITEM_USE_COOLDOWN_MS;
        if (biomeCooldownMs < 0) biomeCooldownMs = DEFAULT_BIOME_COOLDOWN_MS;
        if (blockCooldownMs < 0) blockCooldownMs = DEFAULT_BLOCK_COOLDOWN_MS;
        if (mobCooldownMs < 0) mobCooldownMs = DEFAULT_MOB_COOLDOWN_MS;
        if (passiveMobCooldownMs < 0) passiveMobCooldownMs = DEFAULT_PASSIVE_MOB_COOLDOWN_MS;
        if (structureCooldownMs < 0) structureCooldownMs = DEFAULT_STRUCTURE_COOLDOWN_MS;
        if (pathTrackingCooldownMs < 0) pathTrackingCooldownMs = DEFAULT_PATH_TRACKING_COOLDOWN_MS;

        itemIntensity = clampIntensity(itemIntensity, DEFAULT_ITEM_INTENSITY);
        biomeIntensity = clampIntensity(biomeIntensity, DEFAULT_BIOME_INTENSITY);
        blockIntensity = clampIntensity(blockIntensity, DEFAULT_BLOCK_INTENSITY);
        structureIntensity = clampIntensity(structureIntensity, DEFAULT_STRUCTURE_INTENSITY);
        mobIntensity = clampIntensity(mobIntensity, DEFAULT_MOB_INTENSITY);
    }

    private double clampIntensity(double value, double defaultValue) {
        if (value < 0.0 || value > 1.0) {
            return defaultValue;
        }
        return value;
    }

    public static TriggerSettings defaults() {
        return new TriggerSettings();
    }
}
