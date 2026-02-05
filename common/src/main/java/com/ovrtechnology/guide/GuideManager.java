package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;

/**
 * Central manager for opening the AromaCraft guide.
 * Handles client-side screen opening via reflection to avoid server-side class loading.
 */
public final class GuideManager {

    private GuideManager() {
    }

    /**
     * Opens the guide screen on the client.
     * Safe to call from server-side code through reflection.
     */
    public static void openGuideClient() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            AromaAffect.LOGGER.debug("Cannot open guide: no player");
            return;
        }

        AromaAffect.LOGGER.debug("Opening AromaCraft guide");
        minecraft.execute(() -> {
            GuideBook book = AromaAffectGuideContent.getBook();
            minecraft.setScreen(new GuideScreen(book));
        });
    }
}
