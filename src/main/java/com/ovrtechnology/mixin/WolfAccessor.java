package com.ovrtechnology.mixin;

import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes whether a wolf is still wet after leaving water, until it finishes shaking off. */
@Mixin(Wolf.class)
public interface WolfAccessor {
    @Accessor("isWet")
    boolean aromaaffect$isWet();
}
