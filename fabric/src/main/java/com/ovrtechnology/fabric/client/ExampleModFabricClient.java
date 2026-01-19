package com.ovrtechnology.fabric.client;

import com.ovrtechnology.AromaCraftClient;
import com.ovrtechnology.nose.client.NoseClient;
import net.fabricmc.api.ClientModInitializer;

public final class ExampleModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register shared model layers early (needed for 3D Nose armor rendering)
        NoseClient.init();

        // Initialize client-side systems (menus, keybindings, rendering)
        AromaCraftClient.init();

        // Register Fabric-specific armor renderer for 3D Nose models
        NoseRenderingFabric.init();
    }
}
