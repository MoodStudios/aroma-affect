package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines a scent trigger associated with a mob/entity.
 * 
 * <p><b>PLACEHOLDER - Future implementation.</b></p>
 * 
 * <p>When a player is near the specified entity type,
 * the configured scent will be triggered.</p>
 * 
 * <p>Example JSON:</p>
 * <pre>
 * {
 *   "entity_type": "minecraft:cow",
 *   "scent_name": "Barnyard",
 *   "range": 3,
 *   "priority": "MEDLOW"
 * }
 * </pre>
 */
@Getter
@Setter
@ToString
public class MobTriggerDefinition {
    
    /**
     * Default range for proximity triggers (in blocks).
     */
    public static final int DEFAULT_RANGE = 3;
    
    /**
     * The Minecraft entity type ID (e.g., "minecraft:cow").
     */
    @SerializedName("entity_type")
    private String entityType;
    
    /**
     * The exact OVR scent name to trigger.
     */
    @SerializedName("scent_name")
    private String scentName;
    
    /**
     * Range in blocks for proximity detection.
     */
    @SerializedName("range")
    private int range = DEFAULT_RANGE;
    
    /**
     * Priority level for this trigger.
     */
    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDLOW;

    /**
     * Scent intensity (0.0 to 1.0).
     * If not specified, uses the global mob_intensity from settings.
     */
    @SerializedName("intensity")
    private Double intensity;

    /**
     * Optional comment for documentation in JSON.
     */
    @SerializedName("_comment")
    private String comment;

    /**
     * Default constructor for GSON.
     */
    public MobTriggerDefinition() {
    }

    /**
     * Gets the intensity, falling back to global setting if not specified.
     *
     * @param globalIntensity the global default intensity from TriggerSettings
     * @return intensity value (0.0 to 1.0)
     */
    public double getIntensityOrDefault(double globalIntensity) {
        return intensity != null ? intensity : globalIntensity;
    }

    /**
     * Validates the definition has required fields.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return entityType != null && !entityType.isEmpty()
            && scentName != null && !scentName.isEmpty()
            && range > 0;
    }
}
