package com.ovrtechnology.mob;

import com.ovrtechnology.definition.ScentedDefinition;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Represents a trackable mob/entity definition loaded from JSON.
 *
 * <p>Each mob definition maps a Minecraft entity type to a scent and provides
 * display properties and trigger configuration.</p>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class MobDefinition extends ScentedDefinition {
    public MobDefinition() {
    }

    @Override
    protected int getDefaultRange() {
        return 3;
    }

    @Override
    protected String getFallbackTitle() {
        return "Mob";
    }

    @Override
    protected String getTranslationTitle() {
        return "entity";
    }

    @Override
    protected ScentPriority getDefaultPriority() {
        return ScentPriority.MEDLOW;
    }
}