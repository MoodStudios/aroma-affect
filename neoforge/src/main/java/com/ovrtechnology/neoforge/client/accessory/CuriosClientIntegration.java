package com.ovrtechnology.neoforge.client.accessory;

import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public final class CuriosClientIntegration {

    private CuriosClientIntegration() {}

    public static void init() {
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getAllNoses()) {
            CuriosRendererRegistry.register(supplier.get(), NoseCurioRenderer::new);
        }
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getLegacyItems()) {
            CuriosRendererRegistry.register(supplier.get(), NoseCurioRenderer::new);
        }
    }
}
