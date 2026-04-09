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

    // Colors (OVR purple theme)
    private static final int COLOR_BG = 0xCC0B0D12;
    private static final int COLOR_BORDER = 0xFFA890F0;
    private static final int COLOR_TEXT = 0xFFE8E8E8;
    private static final int COLOR_COUNT = 0xFFA890F0;
    private static final int COLOR_COMPLETE = 0xFF55FF55;

    private TutorialScentCounterHud() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientGuiEvent.RENDER_HUD.register((guiGraphics, tickCounter) -> {
            renderCounter(guiGraphics);
        });
    }

    private static void renderCounter(GuiGraphics guiGraphics) {
        boolean active = TutorialScentCounterNetworking.isClientCounterActive();

        // Animate opacity
        if (active) {
            opacity = Math.min(1.0f, opacity + FADE_SPEED);
        } else {
            opacity = Math.max(0.0f, opacity - FADE_SPEED);
        }

        if (opacity <= 0.0f) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) return;

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
        if (alpha < 4) return;

        // Background
        int bgAlpha = (int) (opacity * 0xCC);
        int bgColor = (bgAlpha << 24) | (COLOR_BG & 0x00FFFFFF);
        guiGraphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);

        // Border
        int borderAlpha = (int) (opacity * 0xFF);
        int borderColor = (borderAlpha << 24) | (COLOR_BORDER & 0x00FFFFFF);
        drawBorder(guiGraphics, x, y, x + boxWidth, y + boxHeight, borderColor);

        // Text: "Scent Counter: " in white
        int textAlpha = (int) (opacity * 0xFF);
        int textColor = (textAlpha << 24) | (COLOR_TEXT & 0x00FFFFFF);
        int countColor = (textAlpha << 24) | ((complete ? COLOR_COMPLETE : COLOR_COUNT) & 0x00FFFFFF);

        int textX = x + PADDING_X;
        int textY = y + PADDING_Y;

        guiGraphics.drawString(font, label, textX, textY, textColor, true);
        guiGraphics.drawString(font, value, textX + labelWidth, textY, countColor, true);
    }

    private static void drawBorder(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        guiGraphics.fill(left, top, right, top + 1, color);
        guiGraphics.fill(left, bottom - 1, right, bottom, color);
        guiGraphics.fill(left, top, left + 1, bottom, color);
        guiGraphics.fill(right - 1, top, right, bottom, color);
    }
}
