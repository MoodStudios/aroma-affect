package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.definition.trigger.DefinitionTrigger;
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
 *   "item_id": "aromaaffect:winter_scent",
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
public class ItemTriggerDefinition extends DefinitionTrigger {
    /**
     * How long the scent should last in game ticks.
     * 20 ticks = 1 second. Use -1 for indefinite.
     */
    @SerializedName("duration_ticks")
    private int durationTicks = getDefaultDurationTicks();

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
     * @param id    the full item ID
     * @param scentName the OVR scent name
     */
    public ItemTriggerDefinition(String id, String scentName) {
        this.id = id;
        this.scentName = scentName;
    }

    /**
     * Gets the cooldown, falling back to default if not specified.
     *
     * @return cooldown in milliseconds
     */
    public long getCooldownMsOrDefault() {
        return cooldownMs != null ? cooldownMs : getDefaultCooldownMS();
    }

    /**
     * Gets the priority, falling back to default if not specified.
     *
     * @return the priority level
     */
    public ScentPriority getPriorityOrDefault() {
        return priority != null ? priority : getDefaultPriority();
    }

    @Override
    protected ScentPriority getDefaultPriority() {
        return ScentPriority.HIGH;
    }

    @Override
    protected String getDefaultTrigger() {
        return "USE";
    }

    /**
     * Default duration in ticks (200 = 10 seconds).
     */
    protected int getDefaultDurationTicks() {
        return 200;
    }

    /**
     * Default cooldown in milliseconds.
     */
    protected int getDefaultCooldownMS() {
        return 5000;
    }
}
