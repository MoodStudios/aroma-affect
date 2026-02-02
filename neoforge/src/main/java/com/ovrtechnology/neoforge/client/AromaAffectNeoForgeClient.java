package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.AromaAffectClient;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.nose.client.NoseClient;
import dev.architectury.registry.registries.RegistrySupplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * NeoForge-specific client initialization for Aroma Affect.
 * Handles client-side setup on the NeoForge platform.
 */
@Mod(value = AromaAffect.MOD_ID, dist = Dist.CLIENT)
public final class AromaAffectNeoForgeClient {
    
    public AromaAffectNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        // Register shared model layers early (before NeoForge fires layer definition events).
        NoseClient.init();

        // Initialize common client systems early so entity renderers exist before the world renders.
        // (If this runs too late, missing renderers can hard-crash the client when entities are present.)
        AromaAffectClient.init();

        modEventBus.addListener(this::onRegisterClientExtensions);

        // Register block outline renderer for X-ray wireframe
        BlockOutlineRendererNeoForge.init();
    }
    
    private void onClientSetup(FMLClientSetupEvent event) {
        // Initialize common client systems directly (not enqueued)
        // This ensures Architectury events are registered at the right time
        AromaAffectClient.init();
    }

    private void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        NoseItemClientExtensions extensions = new NoseItemClientExtensions();
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getAllNoses()) {
            event.registerItem(extensions, supplier.get());
        }
    }
}

