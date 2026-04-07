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
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric-specific armor renderer for the Nose item.
 *
 * <p>Uses three pre-configured immutable model instances to avoid shared
 * mutable state that corrupts deferred renders in multiplayer.</p>
 */
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
            HumanoidModel<HumanoidRenderState> contextModel
    ) {
        if (slot != EquipmentSlot.HEAD) {
            return;
        }

        // Read UUID from the render state (set by EntityRendererMixin during submit).
        // This is more reliable than the global NoseRenderContext because it survives
        // MC 1.21's batched extractRenderState pipeline.
        UUID entityUuid = null;
        if (bipedEntityRenderState instanceof EntityRenderStateAccess access) {
            entityUuid = access.aromaaffect$getEntityUuid();
        }

        UUID localUuid = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getUUID() : null;

        boolean noseEnabled;
        boolean strapEnabled;

        if (entityUuid != null && entityUuid.equals(localUuid)) {
            // Local player — use direct toggle state
            noseEnabled = NoseRenderToggles.isNoseEnabled();
            strapEnabled = noseEnabled && NoseRenderToggles.isStrapEnabled();
        } else if (entityUuid != null) {
            // Remote player — use server-synced preferences cache
            NoseRenderPreferencesManager.NosePrefs remotePrefs =
                    NoseRenderPreferencesManager.getClientPrefsIfPresent(entityUuid);
            if (remotePrefs != null) {
                noseEnabled = remotePrefs.noseEnabled();
                strapEnabled = remotePrefs.strapEnabled();
            } else {
                // Unknown entity — default to visible
                noseEnabled = true;
                strapEnabled = false;
            }
        } else {
            // No UUID available — default to visible
            noseEnabled = true;
            strapEnabled = false;
        }

        // Select the appropriate pre-configured immutable model
        NoseMaskModel model;
        if (!noseEnabled) {
            model = hiddenModel;
        } else {
            model = strapEnabled ? visibleWithStrapModel : visibleNoStrapModel;
        }

        if (!noseEnabled) {
            return;
        }

        Identifier texture = NoseClient.getArmorTexture(stack);
        RenderType renderType = RenderTypes.armorCutoutNoCull(texture);

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
