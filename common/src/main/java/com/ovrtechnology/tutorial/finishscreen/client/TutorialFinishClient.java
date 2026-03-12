package com.ovrtechnology.tutorial.finishscreen.client;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;

/**
 * Client-side entry point for the tutorial finish screen.
 * Called via reflection from TutorialFinishNetworking to maintain client/common code separation.
 */
public final class TutorialFinishClient {

    private TutorialFinishClient() {}

    /**
     * Opens the tutorial finish screen.
     * Called via reflection from TutorialFinishNetworking.
     */
    public static void open() {
        AromaAffect.LOGGER.info("TutorialFinishClient.open() called!");
        try {
            Minecraft.getInstance().setScreen(new TutorialFinishScreen());
            AromaAffect.LOGGER.info("TutorialFinishScreen created successfully!");
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error creating TutorialFinishScreen: ", e);
        }
    }
}
