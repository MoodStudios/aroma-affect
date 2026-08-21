package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.definition.trigger.DefinitionTrigger;
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
public class StructureTriggerDefinition extends DefinitionTrigger {

    /**
     * Default constructor for GSON.
     */
    public StructureTriggerDefinition() {
    }

    @Override
    protected int getDefaultRange() {
        return 50;
    }

    @Override
    protected ScentPriority getDefaultPriority() {
        return ScentPriority.MEDLOW;
    }
}

