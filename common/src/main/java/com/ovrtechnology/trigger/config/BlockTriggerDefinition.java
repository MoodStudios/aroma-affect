package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.definition.trigger.DefinitionTrigger;
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
public class BlockTriggerDefinition extends DefinitionTrigger {

    /**
     * Default constructor for GSON.
     */
    public BlockTriggerDefinition() {
    }

    @Override
    protected int getDefaultRange() {
        return 5;
    }

    @Override
    protected ScentPriority getDefaultPriority() {
        return super.getDefaultPriority();
    }

    @Override
    protected String getDefaultMode() {
        return null;
    }

    @Override
    protected String getDefaultTrigger() {
        return super.getDefaultTrigger();
    }
}
