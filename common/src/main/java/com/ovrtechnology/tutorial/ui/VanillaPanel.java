package com.ovrtechnology.tutorial.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

/**
 * Draws a box with the vanilla tooltip background and frame, so tutorial panels match the game's own UI.
 */
public final class VanillaPanel {

    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("tooltip/background");
    private static final ResourceLocation FRAME = ResourceLocation.withDefaultNamespace("tooltip/frame");

    /** Transparent margin baked into the tooltip sprites around the visible box. */
    private static final int SPRITE_MARGIN = 9;

    /** Vanilla tooltip title colour, for panel headers. */
    public static final int TITLE_COLOR = 0xFFFFFF55;

    private VanillaPanel() {
    }

    public static void draw(GuiGraphics graphics, int left, int top, int right, int bottom) {
        draw(graphics, left, top, right, bottom, 1.0F);
    }

    public static void draw(GuiGraphics graphics, int left, int top, int right, int bottom, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        int color = ARGB.white(Math.min(alpha, 1.0F));
        int x = left - SPRITE_MARGIN;
        int y = top - SPRITE_MARGIN;
        int width = right - left + 2 * SPRITE_MARGIN;
        int height = bottom - top + 2 * SPRITE_MARGIN;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, width, height, color);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, x, y, width, height, color);
    }
}
