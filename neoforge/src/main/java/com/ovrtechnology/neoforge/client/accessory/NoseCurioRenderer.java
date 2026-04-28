package com.ovrtechnology.neoforge.client.accessory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.nose.client.NoseMaskModel;
import com.ovrtechnology.nose.client.NoseModelLayers;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class NoseCurioRenderer implements ICurioRenderer {

    private NoseMaskModel visibleNoStrapModel;
    private NoseMaskModel visibleWithStrapModel;
    private boolean baked = false;

    public NoseCurioRenderer() {}

    private void bakeIfNeeded() {
        if (baked) return;
        var entityModels = Minecraft.getInstance().getEntityModels();

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
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack matrixStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource bufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        LivingEntity entity = slotContext.entity();
        if (entity == null || entity.isInvisible()) return;

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

        if (!noseEnabled) return;

        NoseMaskModel model = strapEnabled ? visibleWithStrapModel : visibleNoStrapModel;
        M parentModel = renderLayerParent.getModel();
        if (parentModel instanceof HumanoidModel<?>) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            HumanoidModel raw = (HumanoidModel) parentModel;
            raw.copyPropertiesTo(model);
        }

        ResourceLocation texture = NoseClient.getArmorTexture(stack);
        model.renderToBuffer(
                matrixStack,
                ItemRenderer.getArmorFoilBuffer(
                        bufferSource, RenderType.armorCutoutNoCull(texture), stack.hasFoil()),
                light,
                OverlayTexture.NO_OVERLAY);
    }
}
