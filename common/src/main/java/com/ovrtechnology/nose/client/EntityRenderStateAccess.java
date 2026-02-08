package com.ovrtechnology.nose.client;

import java.util.UUID;

/**
 * Duck interface applied to {@link net.minecraft.client.renderer.entity.state.EntityRenderState}
 * via mixin, allowing us to carry the entity UUID through the render pipeline.
 *
 * <p>In MC 1.21 the {@code extractRenderState()} phase runs for ALL entities
 * before any {@code submit()} call. A single static UUID holder
 * ({@link NoseRenderContext}) gets overwritten by the last entity processed.
 * By storing the UUID directly in the render state, each entity's submit call
 * can restore the correct UUID.</p>
 */
public interface EntityRenderStateAccess {
    UUID aromaaffect$getEntityUuid();
    void aromaaffect$setEntityUuid(UUID uuid);
}
