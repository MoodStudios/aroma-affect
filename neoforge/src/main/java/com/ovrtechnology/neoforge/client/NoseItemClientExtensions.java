package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.nose.client.NoseMaskModel;
import com.ovrtechnology.nose.client.NoseModelLayers;
import com.ovrtechnology.nose.client.NoseRenderContext;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class NoseItemClientExtensions implements IClientItemExtensions {
    private static volatile NoseMaskModel model;
    private static int debugCounter = 0;
    private static final int DEBUG_INTERVAL = 200;

    private static NoseRenderPreferencesManager.NosePrefs getCurrentEntityPrefs() {
        UUID entityUuid = NoseRenderContext.getCurrentEntityUuid();
        String branch;
        NoseRenderPreferencesManager.NosePrefs result;

        // Check if this UUID belongs to a known REMOTE player (in the prefs cache).
        // Skip the local player UUID — for ourselves we always use the direct toggles.
        UUID localUuid = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getUUID() : null;

        if (entityUuid != null && localUuid != null
                && !entityUuid.equals(localUuid)) {
            // Not the local player — check if it's a known remote player
            NoseRenderPreferencesManager.NosePrefs remotePrefs =
                    NoseRenderPreferencesManager.getClientPrefsIfPresent(entityUuid);
            if (remotePrefs != null) {
                result = remotePrefs;
                branch = "REMOTE_PLAYER";
            } else {
                // Unknown UUID (mob/villager/etc with extractRenderState desync) —
                // treat as local player since only NoseItem wearers reach this code
                boolean noseEnabled = NoseRenderToggles.isNoseEnabled();
                boolean strapEnabled = noseEnabled && NoseRenderToggles.isStrapEnabled();
                result = new NoseRenderPreferencesManager.NosePrefs(noseEnabled, strapEnabled);
                branch = "UNKNOWN_UUID_FALLBACK";
            }
        } else {
            // Local player UUID match, or no UUID, or no local player yet
            boolean noseEnabled = NoseRenderToggles.isNoseEnabled();
            boolean strapEnabled = noseEnabled && NoseRenderToggles.isStrapEnabled();
            result = new NoseRenderPreferencesManager.NosePrefs(noseEnabled, strapEnabled);
            branch = (entityUuid != null && entityUuid.equals(localUuid))
                    ? "LOCAL_PLAYER" : "NO_UUID";
        }

        if (++debugCounter >= DEBUG_INTERVAL) {
            debugCounter = 0;
            AromaAffect.LOGGER.info("[NeoForgeNoseRenderer] branch={}, entityUuid={}, localUuid={}, noseEnabled={}, strapEnabled={}, toggleNose={}, toggleStrap={}",
                    branch, entityUuid, localUuid, result.noseEnabled(), result.strapEnabled(),
                    NoseRenderToggles.isNoseEnabled(), NoseRenderToggles.isStrapEnabled());
        }

        return result;
    }

    @Override
    public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        if (layerType != EquipmentClientInfo.LayerType.HUMANOID) {
            return original;
        }

        NoseMaskModel baked = model;
        if (baked == null) {
            baked = new NoseMaskModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(NoseModelLayers.NOSE_MASK)
            );
            model = baked;
            AromaAffect.LOGGER.info("[NeoForgeNoseRenderer] NoseMaskModel baked for first time");
        }

        NoseRenderPreferencesManager.NosePrefs prefs = getCurrentEntityPrefs();
        if (!prefs.noseEnabled()) {
            // Hide the nose_mask child directly. The equipment layer resets
            // standard HumanoidModel parts (head=true for HEAD slot) but never
            // touches custom children, so this survives the pipeline reset.
            baked.setNoseMaskVisible(false);
            return baked;
        }

        baked.setAllVisible(false);
        baked.head.visible = true;
        baked.hat.visible = true;
        baked.setNoseMaskVisible(true);
        baked.setStrapVisible(prefs.strapEnabled());
        return baked;
    }

    @Override
    public ResourceLocation getArmorTexture(
            ItemStack stack,
            EquipmentClientInfo.LayerType layerType,
            EquipmentClientInfo.Layer layer,
            ResourceLocation defaultTexture
    ) {
        // Always return the nose texture so that, even when the model is
        // invisible, the equipment layer never falls back to a vanilla
        // helmet texture.
        return NoseClient.getArmorTexture(stack);
    }

    @Override
    public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int defaultColor) {
        // Render only the base layer, with no tinting.
        return layerIdx == 0 ? 0xFFFFFFFF : 0;
    }
}
