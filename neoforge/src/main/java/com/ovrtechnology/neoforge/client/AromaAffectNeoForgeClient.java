package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.AromaAffectClient;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.omara.OmaraDeviceRegistry;
import com.ovrtechnology.omara.OmaraDeviceScreen;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.entity.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.entity.sniffer.SnifferScreen;
import dev.architectury.registry.registries.RegistrySupplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
        modEventBus.addListener(this::onRegisterMenuScreens);
        if (ModList.get().isLoaded("curios")) {
            modEventBus.addListener(this::onClientSetup);
        }
    }

    private void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SnifferMenuRegistry.SNIFFER_MENU.get(), SnifferScreen::new);
        event.register(OmaraDeviceRegistry.OMARA_DEVICE_MENU.get(), OmaraDeviceScreen::new);

        // Register block outline renderer for X-ray wireframe
        BlockOutlineRendererNeoForge.init();
    }
    
    private void onClientSetup(FMLClientSetupEvent event) {
        // Register Curios renderer for noses on the main thread.
        event.enqueueWork(
                com.ovrtechnology.neoforge.client.accessory.CuriosClientIntegration::init);
    }

    private void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        NoseItemClientExtensions extensions = new NoseItemClientExtensions();
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getAllNoses()) {
            event.registerItem(extensions, supplier.get());
        }
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getLegacyItems()) {
            event.registerItem(extensions, supplier.get());
        }
    }
}

