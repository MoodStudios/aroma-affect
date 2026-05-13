package com.ovrtechnology.fabric.client;

import com.ovrtechnology.nose.NoseRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

public final class NoseRenderingFabric {
    private NoseRenderingFabric() {
    }

    public static void init() {
        for (Holder<Item> holder : NoseRegistry.getAllNoses()) {
            if (holder.isBound()) {
                ArmorRenderer.register(NoseArmorRenderer::new, holder.value());
            }
        }
        for (Holder<Item> holder : NoseRegistry.getLegacyItems()) {
            if (holder.isBound()) {
                ArmorRenderer.register(NoseArmorRenderer::new, holder.value());
            }
        }
    }
}
