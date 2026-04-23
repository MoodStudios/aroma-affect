package com.ovrtechnology.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.nose.client.NoseMaskModel;
import com.ovrtechnology.nose.client.NoseModelLayers;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import com.ovrtechnology.variant.CustomNoseItem;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class NoseLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private NoseMaskModel hiddenModel;
    private NoseMaskModel visibleNoStrapModel;
    private NoseMaskModel visibleWithStrapModel;
    private boolean baked = false;

    public NoseLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private void bakeIfNeeded() {
        if (baked) return;
        var entityModels = Minecraft.getInstance().getEntityModels();

        hiddenModel = new NoseMaskModel(entityModels.bakeLayer(NoseModelLayers.NOSE_MASK));
        hiddenModel.setAllVisible(false);
        hiddenModel.head.visible = true;
        hiddenModel.hat.visible = true;
        hiddenModel.setNoseMaskVisible(false);

        visibleNoStrapModel = new NoseMaskModel(entityModels.bakeLayer(NoseModelLayers.NOSE_MASK));
        visibleNoStrapModel.setAllVisible(false);
        visibleNoStrapModel.head.visible = true;
        visibleNoStrapModel.hat.visible = true;
        visibleNoStrapModel.setNoseMaskVisible(true);
        visibleNoStrapModel.setStrapVisible(false);

        visibleWithStrapModel =
                new NoseMaskModel(entityModels.bakeLayer(NoseModelLayers.NOSE_MASK));
        visibleWithStrapModel.setAllVisible(false);
        visibleWithStrapModel.head.visible = true;
        visibleWithStrapModel.hat.visible = true;
        visibleWithStrapModel.setNoseMaskVisible(true);
        visibleWithStrapModel.setStrapVisible(true);

        baked = true;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (stack.isEmpty()) return;
        if (!(stack.getItem() instanceof NoseItem)
                && !(stack.getItem() instanceof CustomNoseItem)) {
            return;
        }
        if (player.isInvisible()) return;

        bakeIfNeeded();

        UUID entityUuid = player.getUUID();
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

        if (!noseEnabled) return;

        NoseMaskModel model = strapEnabled ? visibleWithStrapModel : visibleNoStrapModel;
        @SuppressWarnings({"unchecked", "rawtypes"})
        net.minecraft.client.model.HumanoidModel raw =
                (net.minecraft.client.model.HumanoidModel) getParentModel();
        raw.copyPropertiesTo(model);

        ResourceLocation texture = NoseClient.getArmorTexture(stack);
        model.renderToBuffer(
                poseStack,
                ItemRenderer.getArmorFoilBuffer(
                        bufferSource, RenderType.armorCutoutNoCull(texture), stack.hasFoil()),
                packedLight,
                OverlayTexture.NO_OVERLAY);
    }
}
