package com.ovrtechnology.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.variant.CustomNoseItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$skipNoseItem(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof NoseItem || stack.getItem() instanceof CustomNoseItem) {
            ci.cancel();
        }
    }
}
