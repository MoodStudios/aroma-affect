package com.ovrtechnology.tutorial.dream.client;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Client-side renderer for the tutorial dream overlay.
 * <p>
 * Renders a white screen fade effect when the tutorial ending
 * dream sequence is active.
 */
public final class TutorialDreamOverlayClient {

    private static float currentProgress = 0.0f;
    private static float displayProgress = 0.0f;
    private static final float LERP_SPEED = 0.08f; // Slower than portal for dreamy feel

    private static boolean initialized = false;

    private TutorialDreamOverlayClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Register tick handler for smooth interpolation
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (Math.abs(displayProgress - currentProgress) > 0.001f) {
                displayProgress += (currentProgress - displayProgress) * LERP_SPEED;
            } else {
                displayProgress = currentProgress;
            }
        });

        // Register HUD overlay renderer
        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            if (displayProgress > 0.001f) {
                renderDreamOverlay(graphics, displayProgress);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial dream overlay client initialized");
    }

    /**
     * Sets the dream overlay progress.
     *
     * @param progress 0.0 = no overlay, 1.0 = full white screen
     */
    public static void setProgress(float progress) {
        currentProgress = Math.max(0.0f, Math.min(1.0f, progress));
    }

    /**
     * Clears the dream overlay immediately.
     */
    public static void clear() {
        currentProgress = 0.0f;
        displayProgress = 0.0f;
    }

    /**
     * Renders the dream overlay effect — white screen that fades in/out.
     */
    private static void renderDreamOverlay(GuiGraphics graphics, float progress) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Alpha increases with progress
        int alpha = (int) (progress * 255) & 0xFF;

        // White color with calculated alpha
        int color = (alpha << 24) | 0xFFFFFF;

        // Fill screen with white overlay
        graphics.fill(0, 0, screenWidth, screenHeight, color);
    }
}
