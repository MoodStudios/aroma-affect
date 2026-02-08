package com.ovrtechnology.neoforge.client;

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

    private static NoseRenderPreferencesManager.NosePrefs getCurrentEntityPrefs() {
        UUID entityUuid = NoseRenderContext.getCurrentEntityUuid();
        if (entityUuid != null && Minecraft.getInstance().player != null
                && entityUuid.equals(Minecraft.getInstance().player.getUUID())) {
            boolean noseEnabled = NoseRenderToggles.isNoseEnabled();
            boolean strapEnabled = noseEnabled && NoseRenderToggles.isStrapEnabled();
            return new NoseRenderPreferencesManager.NosePrefs(noseEnabled, strapEnabled);
        }
        if (entityUuid != null) {
            return NoseRenderPreferencesManager.getClientPrefs(entityUuid);
        }
        return new NoseRenderPreferencesManager.NosePrefs(
                NoseRenderToggles.isNoseEnabled(), NoseRenderToggles.isStrapEnabled());
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
        }

        NoseRenderPreferencesManager.NosePrefs prefs = getCurrentEntityPrefs();
        if (!prefs.noseEnabled()) {
            return original;
        }

        baked.setAllVisible(false);
        baked.head.visible = true;
        baked.hat.visible = true;
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
        if (!getCurrentEntityPrefs().noseEnabled()) {
            return defaultTexture;
        }
        return NoseClient.getArmorTexture(stack);
    }

    @Override
    public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int defaultColor) {
        if (!getCurrentEntityPrefs().noseEnabled()) {
            return defaultColor;
        }
        // Render only the base layer, with no tinting.
        return layerIdx == 0 ? 0xFFFFFFFF : 0;
    }
}
