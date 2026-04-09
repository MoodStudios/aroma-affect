package com.ovrtechnology.tutorial.scentzone.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import com.ovrtechnology.trigger.client.ScentPuffOverlay;
import com.ovrtechnology.trigger.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side handler for scent zone triggers.
 */
public final class TutorialScentZoneClient {

    private TutorialScentZoneClient() {}

    /**
     * Called via reflection when the server triggers a scent zone for this player.
     */
    public static void onZoneTrigger(String scentName, double intensity, String zoneId) {
        ScentTrigger trigger = ScentTrigger.create(
                scentName,
                ScentTriggerSource.CUSTOM_EVENT,
                ScentPriority.HIGH,
                60, // 3 seconds
                intensity
        );
        boolean triggered = ScentTriggerManager.getInstance().trigger(trigger);

        if (triggered) {
            // Show overlay
            if (ClientConfig.getInstance().isPassivePuffOverlay()) {
                ScentPuffOverlay.onScentPuff(scentName, intensity);
            }

            // Chat message disabled for PAX demo — HUD overlay is enough
        }

        AromaAffect.LOGGER.info("[ScentZone] Zone '{}' triggered scent '{}' (sent: {})", zoneId, scentName, triggered);
    }
}
