package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.nose.client.NoseMaskModel;
import com.ovrtechnology.nose.client.NoseModelLayers;
import com.ovrtechnology.nose.client.NoseRenderToggles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class NoseItemClientExtensions implements IClientItemExtensions {
    private static volatile NoseMaskModel model;

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

        if (!NoseRenderToggles.isNoseEnabled()) {
            return original;
        }

        baked.setAllVisible(false);
        baked.head.visible = true;
        baked.hat.visible = true;
        baked.setStrapVisible(NoseRenderToggles.isStrapEnabled());
        return baked;
    }

    @Override
    public ResourceLocation getArmorTexture(
            ItemStack stack,
            EquipmentClientInfo.LayerType layerType,
            EquipmentClientInfo.Layer layer,
            ResourceLocation defaultTexture
    ) {
        if (!NoseRenderToggles.isNoseEnabled()) {
            return defaultTexture;
        }
        return NoseClient.getArmorTexture(stack);
    }

    @Override
    public int getArmorLayerTintColor(ItemStack stack, EquipmentClientInfo.Layer layer, int layerIdx, int defaultColor) {
        if (!NoseRenderToggles.isNoseEnabled()) {
            return defaultColor;
        }
        // Render only the base layer, with no tinting.
        return layerIdx == 0 ? 0xFFFFFFFF : 0;
    }
}
