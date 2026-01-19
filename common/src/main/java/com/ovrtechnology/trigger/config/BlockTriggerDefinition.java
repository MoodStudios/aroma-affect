package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines a scent trigger associated with a block.
 * 
 * <p><b>PLACEHOLDER - Future implementation.</b></p>
 * 
 * <p>When a player interacts with or is near the specified block,
 * the configured scent will be triggered.</p>
 * 
 * <p>Example JSON:</p>
 * <pre>
 * {
 *   "block_id": "minecraft:campfire",
 *   "scent_name": "Smoky",
 *   "trigger_on": "PROXIMITY",
 *   "range": 5,
 *   "priority": "MEDIUM"
 * }
 * </pre>
 */
@Getter
@Setter
@ToString
public class BlockTriggerDefinition {
    
    /**
     * Default range for proximity triggers (in blocks).
     */
    public static final int DEFAULT_RANGE = 5;
    
    /**
     * The Minecraft block ID (e.g., "minecraft:campfire").
     */
    @SerializedName("block_id")
    private String blockId;
    
    /**
     * The exact OVR scent name to trigger.
     */
    @SerializedName("scent_name")
    private String scentName;
    
    /**
     * Trigger mode: "INTERACT" (on right-click) or "PROXIMITY" (when near).
     */
    @SerializedName("trigger_on")
    private String triggerOn = "PROXIMITY";
    
    /**
     * Range in blocks for proximity triggers.
     */
    @SerializedName("range")
    private int range = DEFAULT_RANGE;
    
    /**
     * Priority level for this trigger.
     */
    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDIUM;
    
    /**
     * Optional comment for documentation in JSON.
     */
    @SerializedName("_comment")
    private String comment;
    
    /**
     * Default constructor for GSON.
     */
    public BlockTriggerDefinition() {
    }
    
    /**
     * Checks if this is a proximity-based trigger.
     * 
     * @return true if trigger_on is PROXIMITY
     */
    public boolean isProximityTrigger() {
        return "PROXIMITY".equalsIgnoreCase(triggerOn);
    }
    
    /**
     * Checks if this is an interaction-based trigger.
     * 
     * @return true if trigger_on is INTERACT
     */
    public boolean isInteractTrigger() {
        return "INTERACT".equalsIgnoreCase(triggerOn);
    }
    
    /**
     * Validates the definition has required fields.
     * 
     * @return true if valid
     */
    public boolean isValid() {
        return blockId != null && !blockId.isEmpty()
            && scentName != null && !scentName.isEmpty();
    }
}
