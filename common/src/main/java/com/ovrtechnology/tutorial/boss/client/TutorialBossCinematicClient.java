package com.ovrtechnology.tutorial.boss.client;

import com.ovrtechnology.AromaAffect;

/**
 * Client-side handler for boss spawn cinematics.
 * <p>
 * Currently disabled - just logs the event.
 */
public final class TutorialBossCinematicClient {

    private static boolean initialized = false;

    private TutorialBossCinematicClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        AromaAffect.LOGGER.debug("Tutorial boss cinematic client initialized");
    }

    /**
     * Called from networking to start a boss cinematic.
     * Currently just logs - cinematic disabled to fix crash.
     */
    public static void playCinematic(String bossType,
                                      double cameraX, double cameraY, double cameraZ,
                                      float yaw, float pitch,
                                      double bossX, double bossY, double bossZ) {
        // Disabled for now - just log
        AromaAffect.LOGGER.info("Boss cinematic triggered for {} at ({}, {}, {})",
                bossType, bossX, bossY, bossZ);
    }

    public static boolean isCinematicActive() {
        return false;
    }
}
