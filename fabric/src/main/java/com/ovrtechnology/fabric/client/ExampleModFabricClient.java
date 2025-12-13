package com.ovrtechnology.fabric.client;

import com.ovrtechnology.AromaCraftClient;
import net.fabricmc.api.ClientModInitializer;

public final class ExampleModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialize client-side systems (menus, keybindings, rendering)
        AromaCraftClient.init();
    }
}
