package com.ovrtechnology.fabric.client;

import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.variant.CustomNoseRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;

public final class NoseRenderingFabric {
    private NoseRenderingFabric() {}

    public static void init() {
        NoseArmorRenderer renderer = new NoseArmorRenderer();
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getAllNoses()) {
            ArmorRenderer.register(renderer, supplier.get());
        }
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getLegacyItems()) {
            ArmorRenderer.register(renderer, supplier.get());
        }
        if (CustomNoseRegistry.getCUSTOM_NOSE().isPresent()) {
            ArmorRenderer.register(renderer, CustomNoseRegistry.getCUSTOM_NOSE().get());
        }
    }
}
