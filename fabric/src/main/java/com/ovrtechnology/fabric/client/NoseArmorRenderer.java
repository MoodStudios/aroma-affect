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
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class NoseArmorRenderer implements ArmorRenderer {
    private NoseMaskModel hiddenModel;
    private NoseMaskModel visibleNoStrapModel;
    private NoseMaskModel visibleWithStrapModel;
    private boolean baked = false;

    public NoseArmorRenderer() {}

    private void bakeIfNeeded() {
        if (baked) return;
        var root = Minecraft.getInstance().getEntityModels().bakeLayer(NoseModelLayers.NOSE_MASK);

        hiddenModel = new NoseMaskModel(root);
        hiddenModel.setAllVisible(false);
        hiddenModel.head.visible = true;
        hiddenModel.hat.visible = true;
        hiddenModel.setNoseMaskVisible(false);

        var root2 = Minecraft.getInstance().getEntityModels().bakeLayer(NoseModelLayers.NOSE_MASK);
        visibleNoStrapModel = new NoseMaskModel(root2);
        visibleNoStrapModel.setAllVisible(false);
        visibleNoStrapModel.head.visible = true;
        visibleNoStrapModel.hat.visible = true;
        visibleNoStrapModel.setNoseMaskVisible(true);
        visibleNoStrapModel.setStrapVisible(false);

        var root3 = Minecraft.getInstance().getEntityModels().bakeLayer(NoseModelLayers.NOSE_MASK);
        visibleWithStrapModel = new NoseMaskModel(root3);
        visibleWithStrapModel.setAllVisible(false);
        visibleWithStrapModel.head.visible = true;
        visibleWithStrapModel.hat.visible = true;
        visibleWithStrapModel.setNoseMaskVisible(true);
        visibleWithStrapModel.setStrapVisible(true);

        baked = true;
        AromaAffect.LOGGER.info("[FabricNoseRenderer] Baked 3 NoseMaskModel instances");
    }

    @Override
    public void render(
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            ItemStack stack,
            LivingEntity entity,
            EquipmentSlot slot,
            int light,
            HumanoidModel<LivingEntity> contextModel) {
        if (slot != EquipmentSlot.HEAD) {
            return;
        }

        bakeIfNeeded();

        UUID entityUuid = entity.getUUID();
        UUID localUuid =
                Minecraft.getInstance().player != null
                        ? Minecraft.getInstance().player.getUUID()
                        : null;

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

        NoseMaskModel model = strapEnabled ? visibleWithStrapModel : visibleNoStrapModel;
        contextModel.copyPropertiesTo(model);

        ResourceLocation texture = NoseClient.getArmorTexture(stack);
        model.renderToBuffer(
                matrices,
                ItemRenderer.getArmorFoilBuffer(
                        vertexConsumers,
                        net.minecraft.client.renderer.RenderType.armorCutoutNoCull(texture),
                        stack.hasFoil()),
                light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }
}
