package com.ovrtechnology.mixin;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.sniffer.SnifferTamingData;
import com.ovrtechnology.entity.sniffer.client.TamedSnifferRenderState;
import com.ovrtechnology.sniffernose.SnifferNoseItem;
import net.minecraft.client.renderer.entity.SnifferRenderer;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnifferRenderer.class)
public class SnifferRendererMixin {

    @Unique
    private static final Identifier SNIFFER_WITH_SADDLE =
            Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/entity/sniffer_with_saddle.png");

    @Unique
    private static final Identifier SNIFFER_WITH_NOSE =
            Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/entity/sniffer_with_nose.png");

    @Unique
    private static final Identifier SNIFFER_WITH_SADDLE_AND_NOSE =
            Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/entity/sniffer_with_saddle_and_nose.png");

    @Inject(method = "createRenderState", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$createRenderState(CallbackInfoReturnable<SnifferRenderState> cir) {
        cir.setReturnValue(new TamedSnifferRenderState());
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/sniffer/Sniffer;Lnet/minecraft/client/renderer/entity/state/SnifferRenderState;F)V",
            at = @At("TAIL"))
    private void aromaaffect$extractRenderState(Sniffer sniffer, SnifferRenderState state, float partialTick, CallbackInfo ci) {
        if (state instanceof TamedSnifferRenderState tamedState) {
            SnifferTamingData data = SnifferTamingData.get(sniffer.getUUID());
            tamedState.hasSaddle = !data.saddleItem.isEmpty() && data.saddleItem.is(Items.SADDLE);
            tamedState.hasNose = !data.decorationItem.isEmpty() && data.decorationItem.getItem() instanceof SnifferNoseItem;
            // Detect swimming mode: tamed, mounted, and in water
            tamedState.isSwimmingMode = sniffer.isInWater() && sniffer.isVehicle() && data.ownerUUID != null;
            if (tamedState.isSwimmingMode) {
                // Force digging animation to final frame (fully crouched) instantly
                state.diggingAnimationState.start(0);
                // Speed up walk animation (leg movement) when swimming
                state.walkAnimationSpeed = Math.max(state.walkAnimationSpeed, 1.0F);
            }
        }
    }

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/SnifferRenderState;)Lnet/minecraft/resources/Identifier;",
            at = @At("HEAD"),
            cancellable = true)
    private void aromaaffect$getCustomTexture(SnifferRenderState state, CallbackInfoReturnable<Identifier> cir) {
        if (state instanceof TamedSnifferRenderState tamedState) {
            if (tamedState.hasSaddle && tamedState.hasNose) {
                cir.setReturnValue(SNIFFER_WITH_SADDLE_AND_NOSE);
            } else if (tamedState.hasSaddle) {
                cir.setReturnValue(SNIFFER_WITH_SADDLE);
            } else if (tamedState.hasNose) {
                cir.setReturnValue(SNIFFER_WITH_NOSE);
            }
        }
        // If neither or not our state, let vanilla handle it
    }
}
