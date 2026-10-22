package com.ovrtechnology.mixin;

import com.ovrtechnology.nose.client.EntityRenderStateAccess;
import com.ovrtechnology.nose.client.NoseRenderContext;
import java.util.UUID;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the entity UUID during {@code extractRenderState()} and stores it
 * in the render state via the {@link EntityRenderStateAccess} duck interface.
 *
 * <p>The stored UUID is later read by {@link LivingEntityRendererMixin} at the
 * HEAD of {@code LivingEntityRenderer.submit()} to set {@link NoseRenderContext}
 * before equipment layers run.</p>
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("HEAD")
    )
    private void aromaaffect$captureEntityUuid(Entity entity, EntityRenderState state,
                                                float partialTick, CallbackInfo ci) {
        if (state instanceof EntityRenderStateAccess access) {
            access.aromaaffect$setEntityUuid(entity.getUUID());
        }
    }
}
