package com.ovrtechnology;

import com.ovrtechnology.menu.MenuKeyBindings;
import lombok.experimental.UtilityClass;

/**
 * Client-side initialization for the AromaCraft mod.
 * This handles all client-only systems like menus, rendering, and keybindings.
 * 
 * <p>This should be called from each platform's client initialization:</p>
 * <ul>
 *   <li>Fabric: {@code ExampleModFabricClient.onInitializeClient()}</li>
 *   <li>NeoForge: FMLClientSetupEvent handler</li>
 * </ul>
 */
@UtilityClass
public final class AromaCraftClient {
    
    private static boolean initialized = false;
    
    /**
     * Initializes all client-side systems.
     * Should be called during client mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaCraft.LOGGER.warn("AromaCraftClient.init() called multiple times!");
            return;
        }
        
        AromaCraft.LOGGER.info("Initializing AromaCraft client...");
        
        // Initialize menu keybindings
        MenuKeyBindings.init();
        
        // TODO: Initialize other client systems
        // - HUD overlays for tracking
        // - Particle effects
        // - Sound manager
        
        initialized = true;
        AromaCraft.LOGGER.info("AromaCraft client initialized successfully!");
    }
}
