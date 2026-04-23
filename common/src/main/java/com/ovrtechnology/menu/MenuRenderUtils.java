package com.ovrtechnology.menu;

import com.ovrtechnology.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public final class MenuRenderUtils {

    private MenuRenderUtils() {}

    public static int withAlpha(int argb, float alphaMul) {
        int a = (argb >>> 24) & 0xFF;
        int rgb = argb & Colors.TRANSPARENT;
        int na = Mth.clamp((int) (a * alphaMul), 0, 255);
        return (na << 24) | rgb;
    }

    public static void renderOutline(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

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

    public static void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, volume, pitch));
        }
    }

    public static void playSound(
            net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound,
            float volume,
            float pitch) {
        playSound(sound.value(), volume, pitch);
    }

    public static void playClickSound() {
        playSound(SoundEvents.UI_BUTTON_CLICK, 0.6f, 1.0f);
    }

    public static void playToggleSound(boolean on) {
        float pitch = on ? 1.3f : 0.9f;
        playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f, pitch);
    }

    public static void playSliderSound() {
        playSound(SoundEvents.UI_BUTTON_CLICK, 0.3f, 1.5f);
    }

    public static void blitScaledNearest(
            GuiGraphics graphics,
            ResourceLocation tex,
            int x,
            int y,
            int destW,
            int destH,
            int srcW,
            int srcH) {
        ResourceLocation cutout = ThumbnailCache.getCutout(tex);
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(cutout);
        texture.setFilter(false, false);
        graphics.blit(cutout, x, y, destW, destH, 0.0f, 0.0f, srcW, srcH, srcW, srcH);
    }
}
