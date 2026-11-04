package com.ovrtechnology.entity.irongolem;

/**
 * Duck interface implemented by {@link net.minecraft.world.entity.animal.golem.IronGolem}
 * via mixin, exposing the synched "has nose" flag to renderers.
 */
public interface IronGolemNoseAccessor {
    boolean aromaaffect$hasNose();
    void aromaaffect$setHasNose(boolean hasNose);
}
