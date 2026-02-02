package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines a scent trigger associated with a structure.
 * 
 * <p>When a player is near a structure, the configured scent will be triggered.</p>
 * 
 * <p>Example JSON:</p>
 * <pre>
 * {
 *   "structure_id": "minecraft:village_plains",
 *   "scent_name": "Kindred",
 *   "mode": "PROXIMITY",
 *   "range": 50,
 *   "priority": "MEDLOW"
 * }
 * </pre>
 */
@Getter
@Setter
@ToString
public class StructureTriggerDefinition {
    
    /**
     * Default range for structure proximity triggers (in blocks).
     * Structures are typically detected from farther away than blocks.
     */
    public static final int DEFAULT_RANGE = 50;
    
    /**
     * The Minecraft structure ID (e.g., "minecraft:village_plains").
     */
    @SerializedName("structure_id")
    private String structureId;
    
    /**
     * The exact OVR scent name to trigger.
     */
    @SerializedName("scent_name")
    private String scentName;
    
    /**
     * Trigger mode: "PROXIMITY" (when near structure).
     */
    @SerializedName("mode")
    private String mode = "PROXIMITY";
    
    /**
     * Range in blocks for proximity triggers.
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
     * If not specified, uses the global structure_intensity from settings.
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
    public StructureTriggerDefinition() {
    }
    
    /**
     * Checks if this is a proximity-based trigger.
     * 
     * @return true if mode is PROXIMITY
     */
    public boolean isProximityTrigger() {
        return "PROXIMITY".equalsIgnoreCase(mode);
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
        return structureId != null && !structureId.isEmpty()
            && scentName != null && !scentName.isEmpty();
    }
}

