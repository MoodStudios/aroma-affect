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
     * Default cooldown for item use triggers (ms).
     */
    public static final long DEFAULT_ITEM_USE_COOLDOWN_MS = 5000;
    
    /**
     * Default cooldown for biome triggers (ms).
     */
    public static final long DEFAULT_BIOME_COOLDOWN_MS = 10000;
    
    /**
     * Default cooldown for block triggers (ms).
     */
    public static final long DEFAULT_BLOCK_COOLDOWN_MS = 5000;
    
    /**
     * Default cooldown for mob proximity triggers (ms).
     */
    public static final long DEFAULT_MOB_COOLDOWN_MS = 5000;
    
    /**
     * Minimum time between any scent trigger (ms).
     * Protects hardware from rapid triggering.
     */
    @SerializedName("global_cooldown_ms")
    private long globalCooldownMs = DEFAULT_GLOBAL_COOLDOWN_MS;
    
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
     * Default constructor with default values.
     */
    public TriggerSettings() {
    }
    
    /**
     * Validates and applies defaults for any invalid values.
     */
    public void validate() {
        if (globalCooldownMs < 0) globalCooldownMs = DEFAULT_GLOBAL_COOLDOWN_MS;
        if (itemUseCooldownMs < 0) itemUseCooldownMs = DEFAULT_ITEM_USE_COOLDOWN_MS;
        if (biomeCooldownMs < 0) biomeCooldownMs = DEFAULT_BIOME_COOLDOWN_MS;
        if (blockCooldownMs < 0) blockCooldownMs = DEFAULT_BLOCK_COOLDOWN_MS;
        if (mobCooldownMs < 0) mobCooldownMs = DEFAULT_MOB_COOLDOWN_MS;
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
