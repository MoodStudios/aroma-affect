package com.ovrtechnology.mixin;

import com.ovrtechnology.nose.client.EntityRenderStateAccess;
import java.util.UUID;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateAccess {

    @Unique private UUID aromaaffect$entityUuid;

    @Override
    public UUID aromaaffect$getEntityUuid() {
        return aromaaffect$entityUuid;
    }

    @Override
    public void aromaaffect$setEntityUuid(UUID uuid) {
        aromaaffect$entityUuid = uuid;
    }
}
