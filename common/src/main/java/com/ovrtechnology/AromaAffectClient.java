package com.ovrtechnology;

import com.ovrtechnology.entity.nosesmith.client.NoseSmithClientRegistry;
import com.ovrtechnology.menu.MenuKeyBindings;
import com.ovrtechnology.nose.client.NoseInventoryUi;
import com.ovrtechnology.search.SearchKeyBindings;
import com.ovrtechnology.guide.AromaGuideTracker;
import com.ovrtechnology.trigger.ScentTriggerHandler;
import com.ovrtechnology.menu.TrackingHud;
import com.ovrtechnology.trigger.client.PassiveModeHud;
import com.ovrtechnology.websocket.OvrWebSocketClient;
import com.ovrtechnology.websocket.WebSocketConfig;
import lombok.experimental.UtilityClass;

/**
 * Client-side initialization for the Aroma Affect mod.
 * This handles all client-only systems like menus, rendering, and keybindings.
 * 
 * <p>This should be called from each platform's client initialization:</p>
 * <ul>
 *   <li>Fabric: {@code AromaAffectFabricClient.onInitializeClient()}</li>
 *   <li>NeoForge: FMLClientSetupEvent handler</li>
 * </ul>
 */
@UtilityClass
public final class AromaAffectClient {
    
    private static boolean initialized = false;
    
    /**
     * Initializes all client-side systems.
     * Should be called during client mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("AromaAffectClient.init() called multiple times!");
            return;
        }
        
        AromaAffect.LOGGER.info("Initializing Aroma Affect client...");
        
        // Initialize entity renderers
        NoseSmithClientRegistry.init();
        
        // Initialize menu keybindings
        MenuKeyBindings.init();
        
        // Initialize search keybindings (for activating search with Nose equipped)
        SearchKeyBindings.init();

        // Inventory UI integration (strap toggle button, etc.)
        NoseInventoryUi.init();

        // Initialize Aroma Guide village tracker (action bar distance + compass)
        AromaGuideTracker.init();

        // Initialize scent trigger handler
        ScentTriggerHandler.init();

        // Initialize passive mode HUD overlay
        PassiveModeHud.init();

        // Initialize tracking HUD (Arrived! notifications + global state tick)
        TrackingHud.init();

        // Initialize OVR WebSocket client for scent hardware integration
        // The connection is optional - the mod works without it
        initWebSocketClient();
        
        // TODO: Initialize other client systems
        // - Particle effects
        // - Sound manager
        
        initialized = true;
        AromaAffect.LOGGER.info("Aroma Affect client initialized successfully!");
    }
    
    /**
     * Initializes the OVR WebSocket client with default configuration.
     * 
     * <p>The WebSocket connection is optional and used for communicating with
     * OVR's scent hardware. The mod works perfectly without it - the connection
     * will simply retry in the background.</p>
     */
    private static void initWebSocketClient() {
        // TODO: Load config from file when configuration system is set up
        // For now, use default config (localhost:8080)
        WebSocketConfig config = WebSocketConfig.builder()
                .host("localhost")
                .port(8080)
                .autoConnect(true)       // Try to connect on startup
                .autoReconnect(true)     // Keep trying if connection fails
                .debugLogging(true)      // DEBUG: enabled for troubleshooting OVR connection
                .healthCheckIntervalMs(5000) // 5 seconds health check interval
                .build();
        
        OvrWebSocketClient.init(config);
        
        // Register a listener to log connection events
        OvrWebSocketClient.getInstance().addConnectionListener(
                new com.ovrtechnology.websocket.WebSocketConnectionListener() {
                    @Override
                    public void onConnected() {
                        AromaAffect.LOGGER.info("[OVR] Connected to scent hardware bridge!");
                    }
                    
                    @Override
                    public void onDisconnected(String reason, boolean wasClean) {
                        if (!wasClean) {
                            AromaAffect.LOGGER.warn("[OVR] Disconnected from hardware bridge: {}", reason);
                        }
                    }
                    
                    @Override
                    public void onReconnecting(int attemptNumber, long delayMs) {
                        if (attemptNumber == 1) {
                            AromaAffect.LOGGER.info("[OVR] Hardware bridge not available, will retry in background...");
                        }
                    }
                }
        );
    }
}

