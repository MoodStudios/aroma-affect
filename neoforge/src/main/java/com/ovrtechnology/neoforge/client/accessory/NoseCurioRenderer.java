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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
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

        visibleWithStrapModel = new NoseMaskModel(entityModels.bakeLayer(NoseModelLayers.NOSE_MASK));
        visibleWithStrapModel.setAllVisible(false);
        visibleWithStrapModel.head.visible = true;
        visibleWithStrapModel.hat.visible = true;
        visibleWithStrapModel.setNoseMaskVisible(true);
        visibleWithStrapModel.setStrapVisible(true);

        baked = true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            MultiBufferSource renderTypeBuffer,
            int packedLight,
            S renderState,
            RenderLayerParent<S, M> renderLayerParent,
            EntityRendererProvider.Context context,
            float yRotation,
            float xRotation) {
        LivingEntity entity = slotContext.entity();
        if (entity == null || entity.isInvisible()) return;
        if (!(renderState instanceof HumanoidRenderState humanoidState)) return;

        bakeIfNeeded();

        UUID entityUuid = entity.getUUID();
        UUID localUuid = Minecraft.getInstance().player != null
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
        // setupAnim(renderState) propagates head pitch/yaw and limb animations from the
        // shared HumanoidRenderState — no need for the old copyPropertiesTo helper.
        model.setupAnim(humanoidState);

        Identifier texture = NoseClient.getArmorTexture(stack);
        model.renderToBuffer(
                poseStack,
                renderTypeBuffer.getBuffer(RenderTypes.armorCutoutNoCull(texture)),
                packedLight,
                OverlayTexture.NO_OVERLAY);
    }
}
