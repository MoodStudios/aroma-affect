package com.ovrtechnology;

import com.ovrtechnology.entity.nosesmith.client.NoseSmithClientRegistry;
import com.ovrtechnology.entity.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.guide.AromaGuideTracker;
import com.ovrtechnology.history.TrackingHistoryData;
import com.ovrtechnology.menu.MenuKeyBindings;
import com.ovrtechnology.menu.TrackingHud;
import com.ovrtechnology.network.NoseRenderNetworking;
import com.ovrtechnology.nose.client.NoseRenderPreferencesManager;
import com.ovrtechnology.search.SearchKeyBindings;
import com.ovrtechnology.trigger.ScentTriggerHandler;
import com.ovrtechnology.trigger.client.PassiveModeHud;
import com.ovrtechnology.trigger.client.PathTrackingMaskOverlay;
import com.ovrtechnology.trigger.client.ScentPuffOverlay;
import com.ovrtechnology.websocket.OvrWebSocketClient;
import com.ovrtechnology.websocket.WebSocketConfig;
import dev.architectury.event.events.client.ClientTickEvent;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class AromaAffectClient {

    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("AromaAffectClient.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing Aroma Affect client...");

        NoseSmithClientRegistry.init();

        SnifferMenuRegistry.initClient();

        MenuKeyBindings.init();

        SearchKeyBindings.init();

        com.ovrtechnology.trigger.config.ClientConfig clientCfg =
                com.ovrtechnology.trigger.config.ClientConfig.getInstance();
        com.ovrtechnology.nose.client.NoseRenderToggles.setNoseEnabled(
                clientCfg.isNoseRenderEnabled());
        com.ovrtechnology.nose.client.NoseRenderToggles.setStrapEnabled(
                clientCfg.isNoseRenderEnabled() && clientCfg.isStrapEnabled());

        initNoseRenderSync();

        AromaGuideTracker.init();

        ScentTriggerHandler.init();

        PassiveModeHud.init();

        TrackingHud.init();

        PathTrackingMaskOverlay.init();

        ScentPuffOverlay.init();

        initWebSocketClient();

        initialized = true;
        AromaAffect.LOGGER.info("Aroma Affect client initialized successfully!");
    }

    private static boolean sentInitialPrefs = false;

    private static boolean lastSentNoseEnabled = true;
    private static boolean lastSentStrapEnabled = false;
    private static java.util.UUID lastPlayerUuid = null;

    private static void initNoseRenderSync() {
        ClientTickEvent.CLIENT_POST.register(
                minecraft -> {
                    if (minecraft.player != null && !sentInitialPrefs) {
                        sentInitialPrefs = true;
                        boolean noseEnabled =
                                com.ovrtechnology.nose.client.NoseRenderToggles.isNoseEnabled();
                        boolean strapEnabled =
                                noseEnabled
                                        && com.ovrtechnology.nose.client.NoseRenderToggles
                                                .isStrapEnabled();
                        java.util.UUID playerUuid = minecraft.player.getUUID();
                        lastPlayerUuid = playerUuid;
                        lastSentNoseEnabled = noseEnabled;
                        lastSentStrapEnabled = strapEnabled;

                        NoseRenderPreferencesManager.setClientPrefs(
                                playerUuid, noseEnabled, strapEnabled);

                        NoseRenderNetworking.sendPrefsToServer(
                                minecraft.player.registryAccess(), noseEnabled, strapEnabled);
                    }
                    if (minecraft.player != null) {
                        java.util.UUID playerUuid = minecraft.player.getUUID();
                        boolean noseEnabled =
                                com.ovrtechnology.nose.client.NoseRenderToggles.isNoseEnabled();
                        boolean strapEnabled =
                                noseEnabled
                                        && com.ovrtechnology.nose.client.NoseRenderToggles
                                                .isStrapEnabled();

                        NoseRenderPreferencesManager.setClientPrefs(
                                playerUuid, noseEnabled, strapEnabled);

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

                        TrackingHistoryData.invalidate();
                    }
                });
    }

    public static void registerCompassProperty() {
        dev.architectury.registry.item.ItemPropertiesRegistry.register(
                com.ovrtechnology.guide.AromaGuideRegistry.getAROMA_GUIDE().get(),
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("angle"),
                new net.minecraft.client.renderer.item.CompassItemPropertyFunction(
                        (level, stack, entity) -> {
                            net.minecraft.world.item.component.LodestoneTracker tracker =
                                    stack.get(
                                            net.minecraft.core.component.DataComponents
                                                    .LODESTONE_TRACKER);
                            return tracker != null ? tracker.target().orElse(null) : null;
                        }));
    }

    private static void initWebSocketClient() {

        WebSocketConfig config =
                WebSocketConfig.builder()
                        .host("127.0.0.1")
                        .port(8080)
                        .autoConnect(true)
                        .autoReconnect(true)
                        .debugLogging(true)
                        .healthCheckIntervalMs(5000)
                        .build();

        OvrWebSocketClient.init(config);

        OvrWebSocketClient.getInstance()
                .addConnectionListener(
                        new com.ovrtechnology.websocket.WebSocketConnectionListener() {
                            @Override
                            public void onConnected() {
                                AromaAffect.LOGGER.info(
                                        "[OVR] Connected to scent hardware bridge!");
                            }

                            @Override
                            public void onDisconnected(String reason, boolean wasClean) {
                                if (!wasClean) {
                                    AromaAffect.LOGGER.warn(
                                            "[OVR] Disconnected from hardware bridge: {}", reason);
                                }
                            }

                            @Override
                            public void onReconnecting(int attemptNumber, long delayMs) {
                                if (attemptNumber == 1) {
                                    AromaAffect.LOGGER.info(
                                            "[OVR] Hardware bridge not available, will retry in background...");
                                }
                            }
                        });
    }
}
