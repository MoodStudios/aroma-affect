package com.ovrtechnology.mixin;

import com.ovrtechnology.entity.sniffer.SnifferTamingData;
import java.util.WeakHashMap;
import net.minecraft.client.model.SnifferModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Pre-1.21.2 there is no render state copy, so the swim pose is applied to the client entity's animation states
@Mixin(SnifferModel.class)
public class SnifferModelMixin {

    @Shadow
    @Final
    private ModelPart head;

    @Unique
    private static final WeakHashMap<Sniffer, Boolean> aromaaffect$wasInSwimMode = new WeakHashMap<>();

    @Unique
    private static boolean aromaaffect$isSwimMode(Sniffer sniffer) {
        if (!sniffer.isInWater() || !sniffer.isVehicle()) {
            return false;
        }
        return SnifferTamingData.get(sniffer.getUUID()).ownerUUID != null;
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/animal/sniffer/Sniffer;FFFFF)V", at = @At("HEAD"))
    private void aromaaffect$forceDigWhenSwimming(Sniffer sniffer, float limbSwing, float limbSwingAmount,
                                                  float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        boolean inSwimMode = aromaaffect$isSwimMode(sniffer);
        boolean wasInSwim = aromaaffect$wasInSwimMode.getOrDefault(sniffer, false);
        if (inSwimMode) {
            sniffer.diggingAnimationState.start(0);
        } else if (wasInSwim) {
            sniffer.diggingAnimationState.stop();
        }
        aromaaffect$wasInSwimMode.put(sniffer, inSwimMode);
    }

    @ModifyVariable(method = "setupAnim(Lnet/minecraft/world/entity/animal/sniffer/Sniffer;FFFFF)V",
            at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float aromaaffect$fastLegsWhenSwimming(float limbSwingAmount, Sniffer sniffer) {
        return aromaaffect$isSwimMode(sniffer) ? Math.max(limbSwingAmount, 1.0F) : limbSwingAmount;
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/animal/sniffer/Sniffer;FFFFF)V", at = @At("TAIL"))
    private void aromaaffect$resetHeadWhenSwimming(Sniffer sniffer, float limbSwing, float limbSwingAmount,
                                                   float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (aromaaffect$isSwimMode(sniffer)) {
            this.head.xRot = headPitch * ((float) Math.PI / 180F);
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        }
    }
}
