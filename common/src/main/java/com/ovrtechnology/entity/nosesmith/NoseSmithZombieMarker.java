package com.ovrtechnology.entity.nosesmith;

/**
 * Duck interface applied to {@link net.minecraft.world.entity.monster.ZombieVillager}
 * via mixin. Allows {@link NoseSmithEntity} to mark a zombie villager as a former
 * Nose Smith during zombification so the cure restores the correct entity type.
 */
public interface NoseSmithZombieMarker {

    void aromaaffect$markAsNoseSmith(boolean hasNose, String requestedFlower, boolean houseDecorated, long noseRemovedGameTime);
}
