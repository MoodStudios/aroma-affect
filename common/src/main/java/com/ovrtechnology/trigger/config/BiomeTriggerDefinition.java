package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines a scent trigger associated with a biome.
 * 
 * <p><b>PLACEHOLDER - Future implementation.</b></p>
 * 
 * <p>When a player enters or remains in the specified biome,
 * the configured scent will be triggered.</p>
 * 
 * <p>Example JSON:</p>
 * <pre>
 * {
 *   "biome_id": "minecraft:forest",
 *   "scent_name": "Evergreen",
 *   "mode": "AMBIENT",
 *   "priority": "MEDLOW"
 * }
 * </pre>
 */
@Getter
@Setter
@ToString
public class BiomeTriggerDefinition {
    
    /**
     * The Minecraft biome ID (e.g., "minecraft:forest").
     */
    @SerializedName("biome_id")
    private String biomeId;
    
    /**
     * The exact OVR scent name to trigger.
     */
    @SerializedName("scent_name")
    private String scentName;
    
    /**
     * Trigger mode: "ENTER" (once on entry) or "AMBIENT" (continuous).
     */
    @SerializedName("mode")
    private String mode = "AMBIENT";
    
    /**
     * Priority level for this trigger.
     */
    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDLOW;
    
    /**
     * Optional comment for documentation in JSON.
     */
    @SerializedName("_comment")
    private String comment;
    
    /**
     * Default constructor for GSON.
     */
    public BiomeTriggerDefinition() {
    }
    
    /**
     * Checks if this is an ambient (continuous) trigger.
     * 
     * @return true if mode is AMBIENT
     */
    public boolean isAmbient() {
        return "AMBIENT".equalsIgnoreCase(mode);
    }
    
    /**
     * Checks if this is an entry trigger (one-time on biome change).
     * 
     * @return true if mode is ENTER
     */
    public boolean isEnterTrigger() {
        return "ENTER".equalsIgnoreCase(mode);
    }
    
    /**
     * Validates the definition has required fields.
     * 
     * @return true if valid
     */
    public boolean isValid() {
        return biomeId != null && !biomeId.isEmpty()
            && scentName != null && !scentName.isEmpty();
    }
}
