package com.ovrtechnology.tutorial.portal.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Client-side renderer for the tutorial portal overlay.
 * <p>
 * Renders a purple pulsing overlay on the screen when the player
 * is inside a tutorial portal area, building up before teleportation.
 */
public final class TutorialPortalOverlayClient {

    private static float currentProgress = 0.0f;
    private static float displayProgress = 0.0f;
    private static final float LERP_SPEED = 0.25f;

    private static boolean initialized = false;

    private TutorialPortalOverlayClient() {
    }

    /**
     * Initializes the portal overlay renderer.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Register tick handler for smooth interpolation
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (ReplayCompat.isInReplay()) return;
            // Smoothly interpolate towards target progress
            if (Math.abs(displayProgress - currentProgress) > 0.001f) {
                displayProgress += (currentProgress - displayProgress) * LERP_SPEED;
            } else {
                displayProgress = currentProgress;
            }
        });

        // Register HUD overlay renderer
        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            if (displayProgress > 0.001f) {
                renderPortalOverlay(graphics, displayProgress);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial portal overlay client initialized");
    }

    /**
     * Sets the portal overlay progress.
     * Called from networking when server sends portal state.
     *
     * @param progress 0.0 = no overlay, 1.0 = full overlay
     */
    public static void setProgress(float progress) {
        currentProgress = Math.max(0.0f, Math.min(1.0f, progress));
    }

    /**
     * Clears the portal overlay immediately.
     */
    public static void clear() {
        currentProgress = 0.0f;
        displayProgress = 0.0f;
    }

    /**
     * Renders the portal overlay effect - purple vignette that intensifies.
     */
    private static void renderPortalOverlay(GuiGraphics graphics, float progress) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Pulsing effect
        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time / 150.0) * 0.1 + 0.9);

        // Alpha increases with progress, with pulse
        int alpha = (int) (progress * pulse * 180) & 0xFF;

        // Purple color (0x8B00FF) with calculated alpha
        int color = (alpha << 24) | 0x6B00AA;

        // Fill screen with purple overlay
        graphics.fill(0, 0, screenWidth, screenHeight, color);
    }
}
