package com.ovrtechnology.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.client.EntityRenderStateAccess;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.nose.client.NoseMaskModel;
import com.ovrtechnology.nose.client.NoseModelLayers;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
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
    private final NoseMaskModel hiddenModel;
    private final NoseMaskModel visibleNoStrapModel;
    private final NoseMaskModel visibleWithStrapModel;

    public NoseArmorRenderer(EntityRendererProvider.Context context) {
        hiddenModel = new NoseMaskModel(context.bakeLayer(NoseModelLayers.NOSE_MASK));
        hiddenModel.setAllVisible(false);
        hiddenModel.head.visible = true;
        hiddenModel.hat.visible = true;
        hiddenModel.setNoseMaskVisible(false);

        visibleNoStrapModel = new NoseMaskModel(context.bakeLayer(NoseModelLayers.NOSE_MASK));
        visibleNoStrapModel.setAllVisible(false);
        visibleNoStrapModel.head.visible = true;
        visibleNoStrapModel.hat.visible = true;
        visibleNoStrapModel.setNoseMaskVisible(true);
        visibleNoStrapModel.setStrapVisible(false);

        visibleWithStrapModel = new NoseMaskModel(context.bakeLayer(NoseModelLayers.NOSE_MASK));
        visibleWithStrapModel.setAllVisible(false);
        visibleWithStrapModel.head.visible = true;
        visibleWithStrapModel.hat.visible = true;
        visibleWithStrapModel.setNoseMaskVisible(true);
        visibleWithStrapModel.setStrapVisible(true);

        AromaAffect.LOGGER.info("[FabricNoseRenderer] Created 3 immutable NoseMaskModel instances");
    }

    @Override
    public void render(
            PoseStack matrices,
            SubmitNodeCollector orderedRenderCommandQueue,
            ItemStack stack,
            HumanoidRenderState bipedEntityRenderState,
            EquipmentSlot slot,
            int light,
            HumanoidModel<HumanoidRenderState> contextModel) {
        if (slot != EquipmentSlot.HEAD) {
            return;
        }

        UUID entityUuid = null;
        if (bipedEntityRenderState instanceof EntityRenderStateAccess access) {
            entityUuid = access.aromaaffect$getEntityUuid();
        }

        UUID localUuid =
                Minecraft.getInstance().player != null
                        ? Minecraft.getInstance().player.getUUID()
                        : null;

        boolean noseEnabled;
        boolean strapEnabled;

        if (entityUuid != null && entityUuid.equals(localUuid)) {

            noseEnabled = NoseRenderToggles.isNoseEnabled();
            strapEnabled = noseEnabled && NoseRenderToggles.isStrapEnabled();
        } else if (entityUuid != null) {

            NoseRenderPreferencesManager.NosePrefs remotePrefs =
                    NoseRenderPreferencesManager.getClientPrefsIfPresent(entityUuid);
            if (remotePrefs != null) {
                noseEnabled = remotePrefs.noseEnabled();
                strapEnabled = remotePrefs.strapEnabled();
            } else {

                noseEnabled = true;
                strapEnabled = false;
            }
        } else {

            noseEnabled = true;
            strapEnabled = false;
        }

        NoseMaskModel model;
        if (!noseEnabled) {
            model = hiddenModel;
        } else {
            model = strapEnabled ? visibleWithStrapModel : visibleNoStrapModel;
        }

        if (!noseEnabled) {
            return;
        }

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
                (ModelFeatureRenderer.CrumblingOverlay) null);
    }
}
