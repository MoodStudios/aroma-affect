package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines a scent trigger associated with an item.
 * 
 * <p>When a player uses the specified item, the configured scent
 * will be triggered on the OVR hardware.</p>
 * 
 * <p>Example JSON:</p>
 * <pre>
 * {
 *   "item_id": "aromacraft:winter_scent",
 *   "scent_name": "Winter",
 *   "trigger_on": "USE",
 *   "duration_ticks": 200,
 *   "priority": "HIGH",
 *   "cooldown_ms": 5000
 * }
 * </pre>
 */
@Getter
@Setter
@ToString
public class ItemTriggerDefinition {
    
    /**
     * Default duration in ticks (200 = 10 seconds).
     */
    public static final int DEFAULT_DURATION_TICKS = 200;
    
    /**
     * Default priority for item triggers.
     */
    public static final ScentPriority DEFAULT_PRIORITY = ScentPriority.HIGH;
    
    /**
     * Default cooldown in milliseconds.
     */
    public static final long DEFAULT_COOLDOWN_MS = 5000;
    
    /**
     * The full item ID including namespace (e.g., "aromacraft:winter_scent").
     */
    @SerializedName("item_id")
    private String itemId;
    
    /**
     * The exact OVR scent name to trigger (e.g., "Winter", "Terra Silva").
     * This value is sent directly to the OVR hardware.
     */
    @SerializedName("scent_name")
    private String scentName;
    
    /**
     * When to trigger the scent.
     * Currently only "USE" is supported.
     */
    @SerializedName("trigger_on")
    private String triggerOn = "USE";
    
    /**
     * How long the scent should last in game ticks.
     * 20 ticks = 1 second. Use -1 for indefinite.
     */
    @SerializedName("duration_ticks")
    private int durationTicks = DEFAULT_DURATION_TICKS;
    
    /**
     * Priority level for this trigger.
     */
    @SerializedName("priority")
    private ScentPriority priority = DEFAULT_PRIORITY;
    
    /**
     * Cooldown in milliseconds before this item can trigger again.
     * If not specified, uses the global item_use_cooldown_ms.
     */
    @SerializedName("cooldown_ms")
    private Long cooldownMs;
    
    /**
     * Default constructor for GSON.
     */
    public ItemTriggerDefinition() {
    }
    
    /**
     * Constructor for programmatic creation.
     * 
     * @param itemId    the full item ID
     * @param scentName the OVR scent name
     */
    public ItemTriggerDefinition(String itemId, String scentName) {
        this.itemId = itemId;
        this.scentName = scentName;
    }
    
    /**
     * Gets the cooldown, falling back to default if not specified.
     * 
     * @return cooldown in milliseconds
     */
    public long getCooldownMsOrDefault() {
        return cooldownMs != null ? cooldownMs : DEFAULT_COOLDOWN_MS;
    }
    
    /**
     * Gets the priority, falling back to default if not specified.
     * 
     * @return the priority level
     */
    public ScentPriority getPriorityOrDefault() {
        return priority != null ? priority : DEFAULT_PRIORITY;
    }
    
    /**
     * Checks if this trigger is for item use (right-click).
     * 
     * @return true if trigger_on is "USE"
     */
    public boolean isUseTriggered() {
        return "USE".equalsIgnoreCase(triggerOn);
    }
    
    /**
     * Validates the definition has required fields.
     * 
     * @return true if valid
     */
    public boolean isValid() {
        return itemId != null && !itemId.isEmpty()
            && scentName != null && !scentName.isEmpty();
    }
}
