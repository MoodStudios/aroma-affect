package com.ovrtechnology.trigger.config;

import com.google.gson.JsonObject;
import com.ovrtechnology.definition.trigger.DefinitionTrigger;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

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

    /** State-dependent scents, checked in order before the default scent. */
    private List<Variant> variants = List.of();

    /** A scent that applies while the mob matches {@code conditions}; see {@link com.ovrtechnology.mob.EntityConditions}. */
    public record Variant(JsonObject conditions, String scentName, String perception) {}

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
