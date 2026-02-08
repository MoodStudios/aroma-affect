package com.ovrtechnology.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.nose.client.NoseMaskModel;
import com.ovrtechnology.nose.client.NoseModelLayers;
import com.ovrtechnology.nose.client.NoseRenderContext;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
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
    private final NoseMaskModel model;
    private static int debugCounter = 0;
    private static final int DEBUG_INTERVAL = 200; // Log every ~200 frames (~3s at 60fps)

    public NoseArmorRenderer(EntityRendererProvider.Context context) {
        this.model = new NoseMaskModel(context.bakeLayer(NoseModelLayers.NOSE_MASK));
        AromaAffect.LOGGER.info("[FabricNoseRenderer] Constructor called — custom ArmorRenderer created");
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

        // Determine nose preferences.
        // In 1.21, extractRenderState runs for all entities before rendering,
        // so NoseRenderContext UUID may belong to a different entity (mob/villager).
        // Only use remote player prefs for UUIDs that are KNOWN remote players.
        UUID entityUuid = NoseRenderContext.getCurrentEntityUuid();
        UUID localUuid = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getUUID() : null;
        boolean noseEnabled;
        boolean strapEnabled;
        String branch;

        if (entityUuid != null && localUuid != null
                && !entityUuid.equals(localUuid)) {
            // Not the local player — check if it's a known remote player
            NoseRenderPreferencesManager.NosePrefs remotePrefs =
                    NoseRenderPreferencesManager.getClientPrefsIfPresent(entityUuid);
            if (remotePrefs != null) {
                noseEnabled = remotePrefs.noseEnabled();
                strapEnabled = remotePrefs.strapEnabled();
                branch = "REMOTE_PLAYER";
            } else {
                // Unknown UUID (mob/villager with extractRenderState desync)
                noseEnabled = NoseRenderToggles.isNoseEnabled();
                strapEnabled = noseEnabled && NoseRenderToggles.isStrapEnabled();
                branch = "UNKNOWN_UUID_FALLBACK";
            }
        } else {
            // Local player or no UUID
            noseEnabled = NoseRenderToggles.isNoseEnabled();
            strapEnabled = noseEnabled && NoseRenderToggles.isStrapEnabled();
            branch = (entityUuid != null && entityUuid.equals(localUuid))
                    ? "LOCAL_PLAYER" : "NO_UUID";
        }

        if (++debugCounter >= DEBUG_INTERVAL) {
            debugCounter = 0;
            AromaAffect.LOGGER.info("[FabricNoseRenderer] branch={}, entityUuid={}, localUuid={}, noseEnabled={}, strapEnabled={}, toggleNose={}, toggleStrap={}",
                    branch, entityUuid, localUuid, noseEnabled, strapEnabled,
                    NoseRenderToggles.isNoseEnabled(), NoseRenderToggles.isStrapEnabled());
        }

        if (!noseEnabled) {
            // Hide nose_mask directly in case the Fabric ArmorRenderer API
            // still allows some vanilla rendering to leak through.
            model.setNoseMaskVisible(false);
            return;
        }

        model.setAllVisible(false);
        model.head.visible = true;
        model.hat.visible = true;
        model.setNoseMaskVisible(true);
        model.setStrapVisible(strapEnabled);

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
                (ModelFeatureRenderer.CrumblingOverlay) null
        );
    }
}
