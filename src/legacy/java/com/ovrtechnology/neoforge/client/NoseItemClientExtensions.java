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
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

// NeoForge 21.1 only renders ArmorItem through the armor layer, so noses are drawn by a dedicated humanoid layer instead
public final class NoseItemClientExtensions implements IClientItemExtensions {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<?> renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer living && living.getModel() instanceof HumanoidModel) {
                living.addLayer(new NoseLayer(living));
            }
        }
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?> renderer = event.getRenderer((EntityType) type);
            if (renderer instanceof LivingEntityRenderer living && living.getModel() instanceof HumanoidModel) {
                living.addLayer(new NoseLayer(living));
            }
        }
    }

    private static final class NoseLayer extends RenderLayer<LivingEntity, HumanoidModel<LivingEntity>> {
        private NoseMaskModel visibleNoStrapModel;
        private NoseMaskModel visibleWithStrapModel;

        private NoseLayer(RenderLayerParent<LivingEntity, HumanoidModel<LivingEntity>> parent) {
            super(parent);
        }

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
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, LivingEntity entity,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            ItemStack stack = entity.getItemBySlot(EquipmentSlot.HEAD);
            if (stack.isEmpty() || !(stack.getItem() instanceof NoseItem || stack.getItem() instanceof CustomNoseItem)) {
                return;
            }
            if (entity.isInvisible()) {
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
            getParentModel().copyPropertiesTo(model);

            ResourceLocation texture = NoseClient.getArmorTexture(stack);
            model.renderToBuffer(
                    poseStack,
                    ItemRenderer.getArmorFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(texture), false),
                    packedLight,
                    OverlayTexture.NO_OVERLAY);
        }
    }
}
