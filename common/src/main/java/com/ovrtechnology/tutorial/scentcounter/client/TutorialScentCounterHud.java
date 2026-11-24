package com.ovrtechnology.tutorial.scentcounter.client;

import com.ovrtechnology.network.TutorialScentCounterNetworking;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Client-side HUD overlay for the scent counter.
 * <p>
 * Displays "Scent Counter: X/16" at the top-right of the screen
 * when activated after the dream end sequence.
 */
public final class TutorialScentCounterHud {

    private static boolean initialized = false;

    // Animation
    private static float opacity = 0.0f;
    private static final float FADE_SPEED = 0.05f;

    // Layout
    private static final int MARGIN_X = 10;
    private static final int MARGIN_Y = 10;
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 6;

    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_GLOW = 0xFFE9B83C;

    // A new scent pops the box, flashes a glow around it and floats a "+1" below it
    private static final long BUMP_MS = 1100;
    private static int shownCount = 0;
    private static long bumpStartMs = 0;
    private static final int COLOR_COUNT = com.ovrtechnology.tutorial.ui.VanillaPanel.TITLE_COLOR;
    private static final int COLOR_COMPLETE = 0xFF55FF55;

    private TutorialScentCounterHud() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientGuiEvent.RENDER_HUD.register((guiGraphics, tickCounter) -> {
            int bottom = renderCounter(guiGraphics);
            renderTimer(guiGraphics, bottom);
        });
    }

    /** Draws the counter and returns the y just below it, or 0 when nothing was drawn. */
    private static int renderCounter(GuiGraphics guiGraphics) {
        boolean active = TutorialScentCounterNetworking.isClientCounterActive();

        // Animate opacity
        if (active) {
            opacity = Math.min(1.0f, opacity + FADE_SPEED);
        } else {
            opacity = Math.max(0.0f, opacity - FADE_SPEED);
        }

        if (opacity <= 0.0f) return 0;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) return 0;

        Font font = minecraft.font;

        int count = TutorialScentCounterNetworking.getClientDetectedCount();
        int max = TutorialScentCounterNetworking.getClientMaxScents();
        boolean complete = count >= max;

        String label = "Scents Discovered: ";
        String value = count + "/" + max;

        int labelWidth = font.width(label);
        int valueWidth = font.width(value);
        int totalTextWidth = labelWidth + valueWidth;

        int boxWidth = PADDING_X * 2 + totalTextWidth;
        int boxHeight = PADDING_Y * 2 + font.lineHeight;

        // Position: top-right
        int screenWidth = guiGraphics.guiWidth();
        int x = screenWidth - boxWidth - MARGIN_X;
        int y = MARGIN_Y;

        int alpha = (int) (opacity * 255);
        if (alpha < 4) return 0;

        long now = System.currentTimeMillis();
        if (count > shownCount) {
            bumpStartMs = now;
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.3f, 0.6f));
        }
        shownCount = count;
        float bump = bumpStartMs == 0 ? 1.0f : Math.min(1.0f, (now - bumpStartMs) / (float) BUMP_MS);

        float centerX = x + boxWidth / 2.0f;
        float centerY = y + boxHeight / 2.0f;
        boolean bumping = bump < 1.0f;
        if (bumping) {
            // Grows and settles back in the first half; the glow and the "+1" last the whole bump
            float pop = (float) Math.sin(Math.PI * Math.min(1.0f, bump * 2.0f));
            float scale = 1.0f + 0.22f * pop;
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(centerX, centerY);
            guiGraphics.pose().scale(scale, scale);
            guiGraphics.pose().translate(-centerX, -centerY);

            float glow = (1.0f - bump) * (1.0f - bump) * opacity;
            int glowAlpha = (int) (glow * 0xC0);
            for (int ring = 3; ring >= 1; ring--) {
                int ringAlpha = glowAlpha / (ring + 1);
                guiGraphics.fill(x - ring, y - ring, x + boxWidth + ring, y + boxHeight + ring,
                        (ringAlpha << 24) | (COLOR_GLOW & 0x00FFFFFF));
            }
        }

        com.ovrtechnology.tutorial.ui.VanillaPanel.draw(guiGraphics, x, y, x + boxWidth, y + boxHeight, opacity);

        // Text: "Scent Counter: " in white
        int textAlpha = (int) (opacity * 0xFF);
        int textColor = (textAlpha << 24) | (COLOR_TEXT & 0x00FFFFFF);
        int countColor = (textAlpha << 24) | ((complete ? COLOR_COMPLETE : COLOR_COUNT) & 0x00FFFFFF);

        int textX = x + PADDING_X;
        int textY = y + PADDING_Y;

        guiGraphics.drawString(font, label, textX, textY, textColor, true);
        guiGraphics.drawString(font, value, textX + labelWidth, textY, countColor, true);

        if (bumping) {
            guiGraphics.pose().popMatrix();

            // "+1" drifting down from under the number, or out to the left when the
            // countdown sits under the counter
            float fade = 1.0f - bump;
            int plusAlpha = (int) (fade * opacity * 0xFF);
            if (plusAlpha > 4) {
                String plus = "+1";
                boolean timerBelow = com.ovrtechnology.network.TutorialFinishNetworking.getClientSecondsLeft() > 0;
                int plusX = timerBelow
                        ? x - font.width(plus) - 4 - (int) (bump * 10)
                        : textX + labelWidth + (valueWidth - font.width(plus)) / 2;
                int plusY = timerBelow ? textY : y + boxHeight + 3 + (int) (bump * 10);
                guiGraphics.drawString(font, plus, plusX, plusY, (plusAlpha << 24) | (COLOR_GLOW & 0x00FFFFFF), true);
            }
        }
        return y + boxHeight;
    }

    private static final int COLOR_TIME = 0xFFFFFFFF;
    private static final int COLOR_TIME_LOW = 0xFFFFD84A;
    private static final int COLOR_TIME_LAST = 0xFFFF5555;
    private static final int TIMER_GAP = 4;

    /**
     * Session countdown, right under the counter (or in its place when the counter is off).
     * It used to be a boss bar at the top, where the hint popups covered it.
     */
    private static void renderTimer(GuiGraphics guiGraphics, int counterBottom) {
        int secondsLeft = com.ovrtechnology.network.TutorialFinishNetworking.getClientSecondsLeft();
        if (secondsLeft <= 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) return;
        Font font = minecraft.font;

        String label = "Time left: ";
        String value = String.format("%d:%02d", secondsLeft / 60, secondsLeft % 60);
        int valueColor = secondsLeft <= 60 ? COLOR_TIME_LAST : secondsLeft <= 180 ? COLOR_TIME_LOW : COLOR_TIME;
        // The last ten seconds blink
        if (secondsLeft <= 10 && (System.currentTimeMillis() / 400) % 2 == 0) {
            valueColor = COLOR_TIME;
        }

        int labelWidth = font.width(label);
        // Width of the widest time this countdown shows, so the box doesn't twitch every second
        int valueWidth = font.width(secondsLeft >= 600 ? "00:00" : "0:00");
        int boxWidth = PADDING_X * 2 + labelWidth + valueWidth;
        int boxHeight = PADDING_Y * 2 + font.lineHeight;

        int x = guiGraphics.guiWidth() - boxWidth - MARGIN_X;
        int y = counterBottom > 0 ? counterBottom + TIMER_GAP : MARGIN_Y;

        com.ovrtechnology.tutorial.ui.VanillaPanel.draw(guiGraphics, x, y, x + boxWidth, y + boxHeight, 1.0f);
        int textX = x + PADDING_X;
        int textY = y + PADDING_Y;
        guiGraphics.drawString(font, label, textX, textY, COLOR_TEXT, true);
        guiGraphics.drawString(font, value, textX + labelWidth + valueWidth - font.width(value), textY, valueColor, true);
    }
}
