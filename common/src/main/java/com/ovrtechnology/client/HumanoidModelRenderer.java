package com.ovrtechnology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.AromaCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface HumanoidModelRenderer {

    default void renderVisible(
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
        render(stack, entity, slotIndex, poseStack, multiBufferSource, light, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
    }

    void render(
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
    );

    static ResourceLocation getTexturePath(String... names) {
        StringBuilder path = new StringBuilder("textures/entity/wearable");
        for (String name : names) {
            path.append('/');
            path.append(name);
        }
        path.append(".png");
        return AromaCraft.id(path.toString());
    }

    static void followBodyRotations(final LivingEntity livingEntity, final HumanoidModel<HumanoidRenderState> model) {
        EntityRenderer<LivingEntity, AvatarRenderState> renderer = (EntityRenderer<LivingEntity, AvatarRenderState>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(livingEntity);

        if (renderer instanceof LivingEntityRenderer) {
            @SuppressWarnings("unchecked")
            LivingEntityRenderer<LivingEntity, AvatarRenderState, EntityModel<AvatarRenderState>> livingRenderer = (LivingEntityRenderer<LivingEntity, AvatarRenderState, EntityModel<AvatarRenderState>>) renderer;
            EntityModel<AvatarRenderState> entityModel = livingRenderer.getModel();

            if (entityModel instanceof HumanoidModel<AvatarRenderState> bipedModel) {
                model.leftArm.loadPose(bipedModel.leftArm.storePose());
                model.rightArm.loadPose(bipedModel.rightArm.storePose());
                model.leftLeg.loadPose(bipedModel.leftLeg.storePose());
                model.rightLeg.loadPose(bipedModel.rightLeg.storePose());
                model.head.loadPose(bipedModel.head.storePose());
                model.body.loadPose(bipedModel.body.storePose());
                model.leftLeg.loadPose(bipedModel.leftLeg.storePose());
                model.rightLeg.loadPose(bipedModel.rightLeg.storePose());
                model.head.loadPose(bipedModel.head.storePose());
                model.body.loadPose(bipedModel.body.storePose());
                // old => bipedModel.copyPropertiesTo(model);
            }
        }
    }
}
