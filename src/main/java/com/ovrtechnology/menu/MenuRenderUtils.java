package com.ovrtechnology.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Shared rendering and sound utilities used across all menu screens.
 */
public final class MenuRenderUtils {

    private MenuRenderUtils() {}

    /** Entity previews use GUI pixels directly, unlike ordinary primitives which apply the pose. */
    public static void renderEntityInViewport(GuiGraphicsExtractor graphics,
            int x0, int y0, int x1, int y1, int size, float offsetY,
            float mouseX, float mouseY, net.minecraft.world.entity.LivingEntity entity) {
        var start = graphics.pose().transformPosition(x0, y0, new org.joml.Vector2f());
        var end = graphics.pose().transformPosition(x1, y1, new org.joml.Vector2f());
        var mouse = graphics.pose().transformPosition(mouseX, mouseY, new org.joml.Vector2f());
        float scale = Math.min((end.x - start.x) / Math.max(1, x1 - x0),
                (end.y - start.y) / Math.max(1, y1 - y0));
        graphics.enableScissor(x0, y0, x1, y1);
        try {
            net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics, Math.round(start.x), Math.round(start.y), Math.round(end.x), Math.round(end.y),
                    Math.max(1, Math.round(size * scale)), offsetY, mouse.x, mouse.y, entity);
        } finally {
            graphics.disableScissor();
        }
    }

    /**
     * Multiplies the alpha channel of an ARGB color by the given factor.
     */
    public static int withAlpha(int argb, float alphaMul) {
        int a = (argb >>> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;
        int na = Mth.clamp((int) (a * alphaMul), 0, 255);
        return (na << 24) | rgb;
    }

    /**
     * Renders a 1-pixel rectangular outline.
     */
    public static void renderOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);           // Top
        graphics.fill(x, y + h - 1, x + w, y + h, color);   // Bottom
        graphics.fill(x, y, x + 1, y + h, color);            // Left
        graphics.fill(x + w - 1, y, x + w, y + h, color);    // Right
    }

    /**
     * Capitalizes the first letter of each whitespace-delimited word.
     */
    public static String capitalizeWords(String str) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Plays a UI sound with the given volume and pitch.
     */
    public static void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, volume, pitch));
        }
    }

    /**
     * Plays a standard button click sound.
     */
    public static void playClickSound() {
        playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.0f);
    }

    /**
     * Plays a toggle sound with pitch varying by on/off state.
     */
    public static void playToggleSound(boolean on) {
        float pitch = on ? 1.3f : 0.9f;
        playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, pitch);
    }

    /**
     * Plays a subtle slider tick sound.
     */
    public static void playSliderSound() {
        playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.5f);
    }
}
