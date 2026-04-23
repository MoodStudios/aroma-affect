package com.ovrtechnology.mixin;

import com.ovrtechnology.nose.client.EntityRenderStateAccess;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("HEAD"))
    private void aromaaffect$captureEntityUuid(
            Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        if (state instanceof EntityRenderStateAccess access) {
            access.aromaaffect$setEntityUuid(entity.getUUID());
        }
    }
}
