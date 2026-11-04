package com.ovrtechnology.fabric.client;

import com.ovrtechnology.nose.NoseRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

public final class NoseRenderingFabric {
    private NoseRenderingFabric() {
    }

    public static void init() {
        //? if <26 {
        /*NoseArmorRenderer renderer = new NoseArmorRenderer();
        *///?}
        for (Holder<Item> holder : NoseRegistry.getAllNoses()) {
            if (holder.isBound()) {
                //? if >=26 {
                ArmorRenderer.register(NoseArmorRenderer::new, holder.value());
                //?} else {
                /*ArmorRenderer.register(renderer, holder.value());
                *///?}
            }
        }
        for (Holder<Item> holder : NoseRegistry.getLegacyItems()) {
            if (holder.isBound()) {
                //? if >=26 {
                ArmorRenderer.register(NoseArmorRenderer::new, holder.value());
                //?} else {
                /*ArmorRenderer.register(renderer, holder.value());
                *///?}
            }
        }
    }
}
