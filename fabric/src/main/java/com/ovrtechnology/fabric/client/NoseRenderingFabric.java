package com.ovrtechnology.fabric.client;

import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;

public final class NoseRenderingFabric {
    private NoseRenderingFabric() {
    }

    public static void init() {
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getAllNoses()) {
            ArmorRenderer.register(NoseArmorRenderer::new, supplier.get());
        }
    }
}

