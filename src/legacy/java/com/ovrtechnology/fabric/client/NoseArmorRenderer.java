package com.ovrtechnology.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.nose.client.NoseMaskModel;
import com.ovrtechnology.nose.client.NoseModelLayers;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class NoseArmorRenderer implements ArmorRenderer {
    private NoseMaskModel visibleNoStrapModel;
    private NoseMaskModel visibleWithStrapModel;

    private void bakeIfNeeded() {
        if (visibleNoStrapModel != null) {
            return;
        }
        var models = Minecraft.getInstance().getEntityModels();

        visibleNoStrapModel = new NoseMaskModel(models.bakeLayer(NoseModelLayers.NOSE_MASK));
        visibleNoStrapModel.setNoseMaskVisible(true);
        visibleNoStrapModel.setStrapVisible(false);

        visibleWithStrapModel = new NoseMaskModel(models.bakeLayer(NoseModelLayers.NOSE_MASK));
        visibleWithStrapModel.setNoseMaskVisible(true);
        visibleWithStrapModel.setStrapVisible(true);

        AromaAffect.LOGGER.info("[FabricNoseRenderer] Baked NoseMaskModel instances");
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack stack, LivingEntity entity,
                       EquipmentSlot slot, int light, HumanoidModel<LivingEntity> contextModel) {
        if (slot != EquipmentSlot.HEAD) {
            return;
        }

        UUID entityUuid = entity.getUUID();
        UUID localUuid = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;

        boolean noseEnabled;
        boolean strapEnabled;
        if (entityUuid.equals(localUuid)) {
            noseEnabled = NoseRenderToggles.isNoseEnabled();
            strapEnabled = noseEnabled && NoseRenderToggles.isStrapEnabled();
        } else {
            NoseRenderPreferencesManager.NosePrefs remotePrefs =
                    NoseRenderPreferencesManager.getClientPrefsIfPresent(entityUuid);
            if (remotePrefs != null) {
                noseEnabled = remotePrefs.noseEnabled();
                strapEnabled = remotePrefs.strapEnabled();
            } else {
                noseEnabled = true;
                strapEnabled = false;
            }
        }

        if (!noseEnabled) {
            return;
        }

        bakeIfNeeded();
        NoseMaskModel model = strapEnabled ? visibleWithStrapModel : visibleNoStrapModel;
        contextModel.copyPropertiesTo(model);

        ResourceLocation texture = NoseClient.getArmorTexture(stack);
        model.renderToBuffer(
                matrices,
                ItemRenderer.getArmorFoilBuffer(vertexConsumers, RenderType.armorCutoutNoCull(texture), false),
                light,
                OverlayTexture.NO_OVERLAY);
    }

    @Override
    public boolean shouldRenderDefaultHeadItem(LivingEntity entity, ItemStack stack) {
        return false;
    }
}
