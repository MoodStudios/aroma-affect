package com.ovrtechnology.mixin;

import com.ovrtechnology.entity.irongolem.IronGolemNoseRenderState;
import com.ovrtechnology.entity.irongolem.IronGolemNoseTracker;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the Iron Golem renderer's render state with our extended version
 * that carries the {@code hasNose} flag to the model layer.
 * Reads nose state from {@link IronGolemNoseTracker} which is synced via networking.
 */
@Mixin(IronGolemRenderer.class)
public class IronGolemRendererMixin {

    @Inject(method = "createRenderState", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$createRenderState(CallbackInfoReturnable<IronGolemRenderState> cir) {
        cir.setReturnValue(new IronGolemNoseRenderState());
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/golem/IronGolem;Lnet/minecraft/client/renderer/entity/state/IronGolemRenderState;F)V",
            at = @At("TAIL"))
    private void aromaaffect$extractRenderState(IronGolem ironGolem, IronGolemRenderState state, float partialTick, CallbackInfo ci) {
        if (state instanceof IronGolemNoseRenderState noseState) {
            noseState.hasNose = IronGolemNoseTracker.hasNose(ironGolem.getUUID());
        }
    }
}
