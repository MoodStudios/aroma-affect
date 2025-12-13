package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.AromaCraft;
import com.ovrtechnology.AromaCraftClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * NeoForge-specific client initialization for AromaCraft.
 * Handles client-side setup on the NeoForge platform.
 */
@Mod(value = AromaCraft.MOD_ID, dist = Dist.CLIENT)
public final class AromaCraftNeoForgeClient {
    
    public AromaCraftNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        // Register client setup event
        modEventBus.addListener(this::onClientSetup);
    }
    
    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize common client systems (menus, keybindings)
            AromaCraftClient.init();
        });
    }
}
