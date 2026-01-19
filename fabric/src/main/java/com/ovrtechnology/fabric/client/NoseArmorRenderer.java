package com.ovrtechnology.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.nose.client.NoseMaskModel;
import com.ovrtechnology.nose.client.NoseModelLayers;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class NoseArmorRenderer implements ArmorRenderer {
    private final NoseMaskModel model;

    public NoseArmorRenderer(EntityRendererProvider.Context context) {
        this.model = new NoseMaskModel(context.bakeLayer(NoseModelLayers.NOSE_MASK));
    }

    @Override
    public void render(
            PoseStack matrices,
            SubmitNodeCollector orderedRenderCommandQueue,
            ItemStack stack,
            HumanoidRenderState bipedEntityRenderState,
            EquipmentSlot slot,
            int light,
            HumanoidModel<HumanoidRenderState> contextModel
    ) {
        if (slot != EquipmentSlot.HEAD) {
            return;
        }

        model.setAllVisible(false);
        model.head.visible = true;
        model.hat.visible = true;
        model.setStrapVisible(NoseRenderToggles.isStrapEnabled());

        ResourceLocation texture = NoseClient.getArmorTexture(stack);
        RenderType renderType = RenderType.armorCutoutNoCull(texture);

        ArmorRenderer.submitTransformCopyingModel(
                contextModel,
                bipedEntityRenderState,
                model,
                bipedEntityRenderState,
                false,
                orderedRenderCommandQueue,
                matrices,
                renderType,
                light,
                OverlayTexture.NO_OVERLAY,
                bipedEntityRenderState.outlineColor,
                (ModelFeatureRenderer.CrumblingOverlay) null
        );
    }
}
