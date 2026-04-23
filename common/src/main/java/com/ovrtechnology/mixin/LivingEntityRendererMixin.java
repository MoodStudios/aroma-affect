package com.ovrtechnology.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.nose.client.EntityRenderStateAccess;
import com.ovrtechnology.nose.client.NoseRenderContext;
import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(
            method =
                    "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"))
    private void aromaaffect$setUuidBeforeLayers(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {
        if (state instanceof EntityRenderStateAccess access) {
            UUID uuid = access.aromaaffect$getEntityUuid();
            if (uuid != null) {
                NoseRenderContext.setCurrentEntityUuid(uuid);
            }
        }
    }
}
