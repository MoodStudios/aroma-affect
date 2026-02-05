package com.ovrtechnology.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Shared rendering and sound utilities used across all menu screens.
 */
public final class MenuRenderUtils {

    private MenuRenderUtils() {}

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
    public static void renderOutline(GuiGraphics graphics, int x, int y, int w, int h, int color) {
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
