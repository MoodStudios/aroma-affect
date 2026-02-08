package com.ovrtechnology.fabric.client;

import com.ovrtechnology.AromaAffectClient;
import com.ovrtechnology.nose.client.NoseClient;
import com.ovrtechnology.omara.OmaraDeviceRegistry;
import com.ovrtechnology.omara.OmaraDeviceScreen;
import com.ovrtechnology.entity.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.entity.sniffer.SnifferScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ExampleModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register shared model layers early (needed for 3D Nose armor rendering)
        NoseClient.init();

        // Initialize client-side systems (menus, keybindings, rendering)
        AromaAffectClient.init();

        // Register Sniffer menu screen
        MenuScreens.register(SnifferMenuRegistry.SNIFFER_MENU.get(), SnifferScreen::new);

        // Register Omara Device menu screen
        MenuScreens.register(OmaraDeviceRegistry.OMARA_DEVICE_MENU.get(), OmaraDeviceScreen::new);

        // Register Fabric-specific armor renderer for 3D Nose models
        NoseRenderingFabric.init();

        // Register block outline renderer for X-ray wireframe
        BlockOutlineRendererFabric.init();
    }
}
