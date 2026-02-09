package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Global settings for the scent trigger system.
 * 
 * <p>These settings control cooldowns and other global behaviors
 * for all trigger types.</p>
 */
@Getter
@Setter
@ToString
public class TriggerSettings {
    
    /**
     * Default global cooldown between any scent triggers (ms).
     */
    public static final long DEFAULT_GLOBAL_COOLDOWN_MS = 3000;
    
    /**
     * Default cooldown per scent name (ms).
     * Used by ScentTriggerManager to prevent the same scent from triggering too frequently.
     */
    public static final long DEFAULT_SCENT_COOLDOWN_MS = 5000;

    /**
     * Default cooldown for item use triggers (ms).
     */
    public static final long DEFAULT_ITEM_USE_COOLDOWN_MS = 5000;
    
    /**
     * Default cooldown for biome triggers (ms).
     * Set to 15 seconds for re-trigger while in biome.
     */
    public static final long DEFAULT_BIOME_COOLDOWN_MS = 15000;

    /**
     * Default cooldown for block triggers (ms).
     * Set to 8 seconds for re-trigger while near block.
     */
    public static final long DEFAULT_BLOCK_COOLDOWN_MS = 5000;

    /**
     * Default cooldown for mob proximity triggers (ms).
     * Set to 8 seconds for re-trigger while near mob.
     */
    public static final long DEFAULT_MOB_COOLDOWN_MS = 8000;

    /**
     * Default cooldown for structure proximity triggers (ms).
     * Set to 30 seconds since structures fire once on entry.
     */
    public static final long DEFAULT_STRUCTURE_COOLDOWN_MS = 30000;

    /**
     * Default cooldown for passive (non-hostile) mob triggers (ms).
     * Longer cooldown since passive mobs don't represent danger.
     */
    public static final long DEFAULT_PASSIVE_MOB_COOLDOWN_MS = 8000;

    /**
     * Default cooldown per scent in passive mode (ms).
     * Prevents the same scent from triggering again too soon.
     * Matches the PassiveModeManager check interval of 300 ticks (15 seconds).
     */
    public static final long DEFAULT_PASSIVE_SCENT_COOLDOWN_MS = 15000;

    /**
     * Default cooldown for path tracking scent triggers (ms).
     * Controls how often scents are emitted while following a path.
     */
    public static final long DEFAULT_PATH_TRACKING_COOLDOWN_MS = 20000;
    
    // ========================================
    // Default intensities by trigger type
    // ========================================
    
    /**
     * Default intensity for item use triggers (more pronounced).
     */
    public static final double DEFAULT_ITEM_INTENSITY = 0.5;
    
    /**
     * Default intensity for biome ambient triggers (subtle).
     */
    public static final double DEFAULT_BIOME_INTENSITY = 0.3;
    
    /**
     * Default intensity for block proximity triggers.
     */
    public static final double DEFAULT_BLOCK_INTENSITY = 0.4;
    
    /**
     * Default intensity for structure proximity triggers.
     */
    public static final double DEFAULT_STRUCTURE_INTENSITY = 0.35;
    
    /**
     * Default intensity for mob proximity triggers.
     */
    public static final double DEFAULT_MOB_INTENSITY = 0.4;
    
    // ========================================
    // Cooldown settings
    // ========================================
    
    /**
     * Minimum time between any scent trigger (ms).
     * Protects hardware from rapid triggering.
     */
    @SerializedName("global_cooldown_ms")
    private long globalCooldownMs = DEFAULT_GLOBAL_COOLDOWN_MS;

    /**
     * Cooldown per scent name (ms).
     * Used by ScentTriggerManager to prevent the same scent from triggering too frequently.
     */
    @SerializedName("scent_cooldown_ms")
    private long scentCooldownMs = DEFAULT_SCENT_COOLDOWN_MS;

    /**
     * Cooldown between item use triggers (ms).
     */
    @SerializedName("item_use_cooldown_ms")
    private long itemUseCooldownMs = DEFAULT_ITEM_USE_COOLDOWN_MS;
    
    /**
     * Cooldown between biome transition triggers (ms).
     */
    @SerializedName("biome_cooldown_ms")
    private long biomeCooldownMs = DEFAULT_BIOME_COOLDOWN_MS;
    
    /**
     * Cooldown between block-based triggers (ms).
     */
    @SerializedName("block_cooldown_ms")
    private long blockCooldownMs = DEFAULT_BLOCK_COOLDOWN_MS;
    
    /**
     * Cooldown between mob proximity triggers (ms).
     */
    @SerializedName("mob_cooldown_ms")
    private long mobCooldownMs = DEFAULT_MOB_COOLDOWN_MS;

    /**
     * Cooldown for passive (non-hostile) mob triggers (ms).
     * Longer than hostile mob cooldown since passive mobs don't represent danger.
     */
    @SerializedName("passive_mob_cooldown_ms")
    private long passiveMobCooldownMs = DEFAULT_PASSIVE_MOB_COOLDOWN_MS;
    
    /**
     * Cooldown between structure proximity triggers (ms).
     */
    @SerializedName("structure_cooldown_ms")
    private long structureCooldownMs = DEFAULT_STRUCTURE_COOLDOWN_MS;

    /**
     * Cooldown per scent in passive mode (ms).
     * Prevents the same scent from triggering again too soon.
     */
    @SerializedName("passive_scent_cooldown_ms")
    private long passiveScentCooldownMs = DEFAULT_PASSIVE_SCENT_COOLDOWN_MS;

    /**
     * Cooldown for path tracking scent triggers (ms).
     * Controls how often scents are emitted while following a path.
     */
    @SerializedName("path_tracking_cooldown_ms")
    private long pathTrackingCooldownMs = DEFAULT_PATH_TRACKING_COOLDOWN_MS;
    
    // ========================================
    // Intensity settings
    // ========================================
    
    /**
     * Default intensity for item use triggers.
     */
    @SerializedName("item_intensity")
    private double itemIntensity = DEFAULT_ITEM_INTENSITY;
    
    /**
     * Default intensity for biome ambient triggers.
     */
    @SerializedName("biome_intensity")
    private double biomeIntensity = DEFAULT_BIOME_INTENSITY;
    
    /**
     * Default intensity for block proximity triggers.
     */
    @SerializedName("block_intensity")
    private double blockIntensity = DEFAULT_BLOCK_INTENSITY;
    
    /**
     * Default intensity for structure proximity triggers.
     */
    @SerializedName("structure_intensity")
    private double structureIntensity = DEFAULT_STRUCTURE_INTENSITY;
    
    /**
     * Default intensity for mob proximity triggers.
     */
    @SerializedName("mob_intensity")
    private double mobIntensity = DEFAULT_MOB_INTENSITY;
    
    /**
     * Default constructor with default values.
     */
    public TriggerSettings() {
    }
    
    /**
     * Validates and applies defaults for any invalid values.
     */
    public void validate() {
        // Validate cooldowns
        if (globalCooldownMs < 0) globalCooldownMs = DEFAULT_GLOBAL_COOLDOWN_MS;
        if (scentCooldownMs < 0) scentCooldownMs = DEFAULT_SCENT_COOLDOWN_MS;
        if (itemUseCooldownMs < 0) itemUseCooldownMs = DEFAULT_ITEM_USE_COOLDOWN_MS;
        if (biomeCooldownMs < 0) biomeCooldownMs = DEFAULT_BIOME_COOLDOWN_MS;
        if (blockCooldownMs < 0) blockCooldownMs = DEFAULT_BLOCK_COOLDOWN_MS;
        if (mobCooldownMs < 0) mobCooldownMs = DEFAULT_MOB_COOLDOWN_MS;
        if (passiveMobCooldownMs < 0) passiveMobCooldownMs = DEFAULT_PASSIVE_MOB_COOLDOWN_MS;
        if (structureCooldownMs < 0) structureCooldownMs = DEFAULT_STRUCTURE_COOLDOWN_MS;
        if (passiveScentCooldownMs < 0) passiveScentCooldownMs = DEFAULT_PASSIVE_SCENT_COOLDOWN_MS;
        if (pathTrackingCooldownMs < 0) pathTrackingCooldownMs = DEFAULT_PATH_TRACKING_COOLDOWN_MS;

        // Validate intensities (clamp to 0.0 - 1.0)
        itemIntensity = clampIntensity(itemIntensity, DEFAULT_ITEM_INTENSITY);
        biomeIntensity = clampIntensity(biomeIntensity, DEFAULT_BIOME_INTENSITY);
        blockIntensity = clampIntensity(blockIntensity, DEFAULT_BLOCK_INTENSITY);
        structureIntensity = clampIntensity(structureIntensity, DEFAULT_STRUCTURE_INTENSITY);
        mobIntensity = clampIntensity(mobIntensity, DEFAULT_MOB_INTENSITY);
    }
    
    /**
     * Clamps intensity to valid range (0.0 - 1.0).
     */
    private double clampIntensity(double value, double defaultValue) {
        if (value < 0.0 || value > 1.0) {
            return defaultValue;
        }
        return value;
    }
    
    /**
     * Creates settings with all default values.
     * 
     * @return default settings instance
     */
    public static TriggerSettings defaults() {
        return new TriggerSettings();
    }
}
