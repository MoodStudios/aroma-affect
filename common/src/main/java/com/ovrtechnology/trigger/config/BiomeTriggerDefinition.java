package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.definition.trigger.DefinitionTrigger;
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
public class BiomeTriggerDefinition extends DefinitionTrigger {

    /**
     * Default constructor for GSON.
     */
    public BiomeTriggerDefinition() {
    }

    @Override
    protected ScentPriority getDefaultPriority() {
        return ScentPriority.MEDLOW;
    }
}
