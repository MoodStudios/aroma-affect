package com.ovrtechnology.trigger.config;

import com.ovrtechnology.definition.trigger.DefinitionTrigger;
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
public class MobTriggerDefinition extends DefinitionTrigger {
    /**
     * Default constructor for GSON.
     */
    public MobTriggerDefinition() {
    }

    @Override
    protected int getDefaultRange() {
        return 3;
    }

    @Override
    protected ScentPriority getDefaultPriority() {
        return ScentPriority.MEDLOW;
    }
}
