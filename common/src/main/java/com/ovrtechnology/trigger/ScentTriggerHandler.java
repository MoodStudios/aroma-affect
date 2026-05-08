package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.event.PlayerStateTickHandler;
import dev.architectury.event.events.client.ClientTickEvent;

public final class ScentTriggerHandler {

    private static boolean initialized = false;

    private ScentTriggerHandler() {}

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("ScentTriggerHandler.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("[ScentTriggerHandler] Starting initialization...");

        try {

            ClientTickEvent.CLIENT_POST.register(
                    minecraft -> {
                        ScentTriggerManager.getInstance().tick();

                        if (minecraft.player != null) {
                            PassiveModeManager.tick(minecraft.player);
                            PlayerStateTickHandler.tick(minecraft.player);
                        }
                    });

            AromaAffect.LOGGER.info(
                    "[ScentTriggerHandler] ClientTickEvent.CLIENT_POST registered successfully!");
        } catch (Exception e) {
            AromaAffect.LOGGER.error(
                    "[ScentTriggerHandler] Failed to register ClientTickEvent!", e);
        }

        initialized = true;
        AromaAffect.LOGGER.info("[ScentTriggerHandler] Initialization complete");
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
