package com.ovrtechnology.tutorial.popupzone.client;

import com.ovrtechnology.network.TutorialPopupNetworking;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Client-side HUD overlay for tutorial popup zones.
 * <p>
 * Renders an informative popup box at the top-left of the screen
 * with smooth fade-in/fade-out animation.
 */
public final class TutorialPopupHudOverlay {

    private static boolean initialized = false;

    // Animation state
    private static float opacity = 0.0f;
    private static final float FADE_SPEED = 0.1f; // per tick

    // Layout
    private static final int MARGIN_X = 10;
    private static final int MARGIN_Y = 10;
    private static final int PADDING = 8;
    private static final int MAX_WIDTH = 220;
    private static final int ICON_SIZE = 10;

    // Colors
    private static final int COLOR_BG = 0xCC0B0D12;
    private static final int COLOR_BORDER = 0xFFA890F0; // OVR Purple
    private static final int COLOR_TEXT = 0xFFE8E8E8;
    private static final int COLOR_ICON = 0xFFA890F0;

    private TutorialPopupHudOverlay() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientGuiEvent.RENDER_HUD.register((guiGraphics, tickCounter) -> {
            renderPopup(guiGraphics);
        });
    }

    private static void renderPopup(GuiGraphics guiGraphics) {
        boolean hasPopup = TutorialPopupNetworking.hasClientPopup();

        // Animate opacity
        if (hasPopup) {
            opacity = Math.min(1.0f, opacity + FADE_SPEED);
        } else {
            opacity = Math.max(0.0f, opacity - FADE_SPEED);
        }

        if (opacity <= 0.0f) {
            return;
        }

        String text = TutorialPopupNetworking.getClientPopupText();
        if (text.isEmpty() && opacity <= 0.01f) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        // Don't render over screens (menus, inventories, etc.)
        if (minecraft.screen != null) {
            return;
        }

        int alpha = (int) (opacity * 255);
        if (alpha < 4) return;

        // Wrap text
        List<FormattedCharSequence> lines = font.split(Component.literal(text), MAX_WIDTH - PADDING * 2 - ICON_SIZE - 4);
        if (lines.isEmpty()) return;

        int lineHeight = font.lineHeight + 2;
        int textHeight = lines.size() * lineHeight;
        int boxWidth = MAX_WIDTH;
        int boxHeight = PADDING * 2 + Math.max(textHeight, ICON_SIZE);

        int x = MARGIN_X;
        int y = MARGIN_Y;

        // Background with alpha
        int bgAlpha = (int) (opacity * 0xCC);
        int bgColor = (bgAlpha << 24) | (COLOR_BG & 0x00FFFFFF);
        guiGraphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);

        // Border with alpha
        int borderAlpha = (int) (opacity * 0xFF);
        int borderColor = (borderAlpha << 24) | (COLOR_BORDER & 0x00FFFFFF);
        drawBorder(guiGraphics, x, y, x + boxWidth, y + boxHeight, borderColor);

        // Info icon "i" in a circle
        int iconX = x + PADDING;
        int iconY = y + PADDING;
        int iconAlpha = (int) (opacity * 0xFF);
        int iconColor = (iconAlpha << 24) | (COLOR_ICON & 0x00FFFFFF);

        // Draw circle-ish background for icon
        int iconCenterX = iconX + ICON_SIZE / 2;
        int iconCenterY = iconY + ICON_SIZE / 2;
        guiGraphics.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, (iconAlpha / 3 << 24) | (COLOR_ICON & 0x00FFFFFF));
        guiGraphics.drawString(font, "i", iconCenterX - 2, iconCenterY - 4, iconColor, true);

        // Draw text lines
        int textX = iconX + ICON_SIZE + 4;
        int textY = y + PADDING;
        int textAlpha = (int) (opacity * 0xFF);
        int textColor = (textAlpha << 24) | (COLOR_TEXT & 0x00FFFFFF);

        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, textX, textY, textColor, true);
            textY += lineHeight;
        }
    }

    private static void drawBorder(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        guiGraphics.fill(left, top, right, top + 1, color);
        guiGraphics.fill(left, bottom - 1, right, bottom, color);
        guiGraphics.fill(left, top, left + 1, bottom, color);
        guiGraphics.fill(right - 1, top, right, bottom, color);
    }
}
