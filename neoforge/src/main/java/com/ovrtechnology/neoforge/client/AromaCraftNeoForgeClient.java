package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.AromaCraft;
import com.ovrtechnology.AromaCraftClient;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.nose.client.NoseClient;
import dev.architectury.registry.registries.RegistrySupplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * NeoForge-specific client initialization for AromaCraft.
 * Handles client-side setup on the NeoForge platform.
 */
@Mod(value = AromaCraft.MOD_ID, dist = Dist.CLIENT)
public final class AromaCraftNeoForgeClient {
    
    public AromaCraftNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        // Register shared model layers early (before NeoForge fires layer definition events).
        NoseClient.init();

        // Initialize common client systems early so entity renderers exist before the world renders.
        // (If this runs too late, missing renderers can hard-crash the client when entities are present.)
        AromaCraftClient.init();

        modEventBus.addListener(this::onRegisterClientExtensions);
    }

    private void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        NoseItemClientExtensions extensions = new NoseItemClientExtensions();
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getAllNoses()) {
            event.registerItem(extensions, supplier.get());
        }
    }
}
