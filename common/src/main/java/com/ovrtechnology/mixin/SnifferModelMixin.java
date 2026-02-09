package com.ovrtechnology.mixin;

import com.ovrtechnology.entity.sniffer.client.TamedSnifferRenderState;
import net.minecraft.client.model.SnifferModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnifferModel.class)
public class SnifferModelMixin {

    @Shadow
    @Final
    private ModelPart head;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/SnifferRenderState;)V", at = @At("TAIL"))
    private void aromaaffect$resetHeadWhenSwimming(SnifferRenderState state, CallbackInfo ci) {
        if (state instanceof TamedSnifferRenderState tamedState && tamedState.isSwimmingMode) {
            // Reset head to follow player look direction instead of digging motion
            this.head.xRot = state.xRot * ((float) Math.PI / 180F);
            this.head.yRot = state.yRot * ((float) Math.PI / 180F);
        }
    }
}
