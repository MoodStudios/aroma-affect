package com.ovrtechnology.fabric.client;

import com.ovrtechnology.AromaCraftClient;
import com.ovrtechnology.client.Renderer;
import net.fabricmc.api.ClientModInitializer;

public final class ExampleModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialize client-side systems (menus, keybindings, rendering)
        AromaCraftClient.init();

        // Global client renderer
        Renderer.register();
    }
}
