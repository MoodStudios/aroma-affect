package com.ovrtechnology.tutorial.intro.client;

import net.minecraft.client.Minecraft;

/**
 * Client-side entry point for the tutorial intro screen.
 * Called via reflection from TutorialIntroNetworking to maintain client/common code separation.
 */
public final class TutorialIntroClient {

    private TutorialIntroClient() {}

    /**
     * Opens the tutorial intro screen.
     * Called via reflection from TutorialIntroNetworking.
     */
    public static void open() {
        Minecraft.getInstance().setScreen(new TutorialIntroScreen());
    }
}
