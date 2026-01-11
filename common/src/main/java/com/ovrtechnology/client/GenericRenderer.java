package com.ovrtechnology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.ItemStack;

public class GenericRenderer implements HumanoidModelRenderer {

    private final ResourceLocation texture;
    private final HumanoidModel<HumanoidRenderState> model;

    public GenericRenderer(String name, HumanoidModel<HumanoidRenderState> model) {
        this(HumanoidModelRenderer.getTexturePath(name), model);
    }

    public GenericRenderer(ResourceLocation texture, HumanoidModel<HumanoidRenderState> model) {
        this.texture = texture;
        this.model = model;
    }

    protected ResourceLocation getTexture() {
        return texture;
    }

    protected HumanoidModel<HumanoidRenderState> getModel() {
        return model;
    }

    @Override
    public void render(
            ItemStack stack,
            LivingEntity entity,
            int slotIndex,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        poseStack.pushPose();
        HumanoidModel<HumanoidRenderState> model = getModel();

        // todo model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // todo model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
        HumanoidModelRenderer.followBodyRotations(entity, model);

        if (entity instanceof Ghast) {
            model.head.yRot = model.body.yRot;
            poseStack.scale(2.5F, 2.5F, 2.5F);
            poseStack.translate(0, -2.5/16F, 0);
        }

        render(poseStack, multiBufferSource, light, stack.hasFoil());
        poseStack.popPose();
    }

    protected void render(PoseStack matrixStack, MultiBufferSource buffer, int light, boolean hasFoil) {
        RenderType renderType = model.renderType(getTexture());
        VertexConsumer vertexBuilder = ItemRenderer.getFoilBuffer(buffer, renderType, false, hasFoil);
        model.renderToBuffer(matrixStack, vertexBuilder, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
