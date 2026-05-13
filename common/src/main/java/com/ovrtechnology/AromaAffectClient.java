package com.ovrtechnology;

import com.ovrtechnology.entity.nosesmith.client.NoseSmithClientRegistry;
import com.ovrtechnology.entity.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.menu.MenuKeyBindings;
import com.ovrtechnology.search.SearchKeyBindings;
import com.ovrtechnology.guide.AromaGuideTracker;
import com.ovrtechnology.trigger.ScentTriggerHandler;
import com.ovrtechnology.menu.TrackingHud;
import com.ovrtechnology.trigger.client.PassiveModeHud;
import com.ovrtechnology.trigger.client.PathTrackingMaskOverlay;
import com.ovrtechnology.trigger.client.ScentPuffOverlay;
import com.ovrtechnology.network.NoseRenderNetworking;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.history.TrackingHistoryData;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.websocket.OvrWebSocketClient;
import com.ovrtechnology.websocket.WebSocketConfig;
import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import lombok.experimental.UtilityClass;
import net.blay09.mods.balm.client.BalmClientRegistrars;
import net.minecraft.client.Minecraft;

/**
 * Client-side initialization for the Aroma Affect mod.
 * This handles all client-only systems like menus, rendering, and keybindings.
 *
 * <p>Invoked via {@code BalmClient.initializeMod(MOD_ID, loadContext, AromaAffectClient::initialize)}
 * from each platform client entry point.</p>
 */
@UtilityClass
public final class AromaAffectClient {

    private static boolean initialized = false;

    /**
     * Initializes all client-side systems.
     * Should be called during client mod initialization.
     */
    public static void initialize(BalmClientRegistrars registrars) {
        if (initialized) {
            AromaAffect.LOGGER.warn("AromaAffectClient.initialize() called multiple times!");
            return;
        }
        
        AromaAffect.LOGGER.info("Initializing Aroma Affect client...");

        // Model layers (NoseMaskModel) — must be registered before any render call
        registrars.modelLayers(com.ovrtechnology.nose.client.NoseClient::registerModelLayers);

        // Entity renderers (NoseSmith)
        registrars.entityRenderers(NoseSmithClientRegistry::register);

        // Menu screens (Sniffer + Omara device). The MenuType holders were
        // populated by phase 4 registrar callbacks earlier in the bootstrap.
        registrars.menuScreens(menus -> {
            menus.register(
                    castMenuHolder(com.ovrtechnology.entity.sniffer.SnifferMenuRegistry.SNIFFER_MENU),
                    com.ovrtechnology.entity.sniffer.SnifferScreen::new);
            menus.register(
                    castMenuHolder(com.ovrtechnology.omara.OmaraDeviceRegistry.OMARA_DEVICE_MENU),
                    com.ovrtechnology.omara.OmaraDeviceScreen::new);
        });

        // Register menu + search keybinds via Balm and start the polling tick handler
        registrars.keyMappings(MenuKeyBindings::register);
        registrars.keyMappings(SearchKeyBindings::register);
        MenuKeyBindings.initTickHandler();
        SearchKeyBindings.initTickHandler();

        // Sync nose render state from persisted config
        com.ovrtechnology.trigger.config.ClientConfig clientCfg = com.ovrtechnology.trigger.config.ClientConfig.getInstance();
        com.ovrtechnology.nose.client.NoseRenderToggles.setNoseEnabled(clientCfg.isNoseRenderEnabled());
        com.ovrtechnology.nose.client.NoseRenderToggles.setStrapEnabled(clientCfg.isNoseRenderEnabled() && clientCfg.isStrapEnabled());

        // Send nose render preferences to server when joining a world,
        // and clean up cache when leaving
        initNoseRenderSync();

        // Initialize Aroma Guide village tracker (action bar distance + compass)
        AromaGuideTracker.init();

        // Initialize scent trigger handler
        ScentTriggerHandler.init();

        // Initialize passive mode HUD overlay
        PassiveModeHud.init();

        // Initialize tracking HUD (Arrived! notifications + global state tick)
        TrackingHud.init();

        // Initialize active-tracking scent mask overlay (fullscreen pulse on each path puff)
        PathTrackingMaskOverlay.init();

        // Initialize general scent puff overlay (Omara Device, etc.)
        ScentPuffOverlay.init();

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
     * Helper to bridge {@code Holder<MenuType<?>>} (the wildcard form stored in
     * registries) with {@code Holder<MenuType<TMenu>>} required by
     * {@code BalmMenuScreenRegistrar.register}. Safe because the menu type was
     * built with the matching menu class in phase 4.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <TMenu extends net.minecraft.world.inventory.AbstractContainerMenu>
            net.minecraft.core.Holder<net.minecraft.world.inventory.MenuType<TMenu>>
            castMenuHolder(net.minecraft.core.Holder<net.minecraft.world.inventory.MenuType<?>> holder) {
        return (net.minecraft.core.Holder) holder;
    }
    
    /**
     * Registers a client tick handler that sends nose render preferences
     * to the server when the local player joins a world, and clears the
     * client-side preference cache when leaving.
     */
    private static boolean sentInitialPrefs = false;
    private static boolean lastSentNoseEnabled = true;
    private static boolean lastSentStrapEnabled = false;
    private static java.util.UUID lastPlayerUuid = null;

    private static void initNoseRenderSync() {
        ClientTickCallback.AFTER.register(minecraft -> {
            if (minecraft.player != null && !sentInitialPrefs) {
                sentInitialPrefs = true;
                boolean noseEnabled = com.ovrtechnology.nose.client.NoseRenderToggles.isNoseEnabled();
                boolean strapEnabled = noseEnabled && com.ovrtechnology.nose.client.NoseRenderToggles.isStrapEnabled();
                java.util.UUID playerUuid = minecraft.player.getUUID();
                lastPlayerUuid = playerUuid;
                lastSentNoseEnabled = noseEnabled;
                lastSentStrapEnabled = strapEnabled;

                // Update local client cache for our own player
                NoseRenderPreferencesManager.setClientPrefs(
                        playerUuid, noseEnabled, strapEnabled);

                // Tell the server so it can broadcast to other players
                NoseRenderNetworking.sendPrefsToServer(
                        minecraft.player.registryAccess(), noseEnabled, strapEnabled);
            }
            if (minecraft.player != null) {
                java.util.UUID playerUuid = minecraft.player.getUUID();
                boolean noseEnabled = com.ovrtechnology.nose.client.NoseRenderToggles.isNoseEnabled();
                boolean strapEnabled = noseEnabled && com.ovrtechnology.nose.client.NoseRenderToggles.isStrapEnabled();

                // Keep own cache entry fresh for local third-person rendering paths.
                NoseRenderPreferencesManager.setClientPrefs(playerUuid, noseEnabled, strapEnabled);

                // Re-sync if preferences changed (or player instance changed).
                if (!sentInitialPrefs
                        || lastPlayerUuid == null
                        || !lastPlayerUuid.equals(playerUuid)
                        || noseEnabled != lastSentNoseEnabled
                        || strapEnabled != lastSentStrapEnabled) {
                    sentInitialPrefs = true;
                    lastPlayerUuid = playerUuid;
                    lastSentNoseEnabled = noseEnabled;
                    lastSentStrapEnabled = strapEnabled;
                    NoseRenderNetworking.sendPrefsToServer(
                            minecraft.player.registryAccess(), noseEnabled, strapEnabled);
                }
            }
            if (minecraft.player == null && sentInitialPrefs) {
                sentInitialPrefs = false;
                lastPlayerUuid = null;
                NoseRenderPreferencesManager.clearClientCache();
                // Flush and invalidate per-world tracking history so the next
                // world/server loads its own file.
                TrackingHistoryData.invalidate();
            }
        });
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
                .host("127.0.0.1")
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

