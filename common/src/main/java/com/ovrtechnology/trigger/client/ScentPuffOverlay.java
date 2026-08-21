package com.ovrtechnology.trigger.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.category.CategoryDefinition;
import com.ovrtechnology.category.CategoryDefinitionLoader;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.util.ColorHelper;
import com.ovrtechnology.util.ResourceUtil;
import net.blay09.mods.balm.client.platform.event.callback.RenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Fullscreen scent mask overlay for non-tracking puffs (Omara Device, etc.).
 *
 * <p>Shows the scent's border/corner image for ~2 seconds with fade-in/fade-out.
 * This is independent of the path tracking system and does not require an active
 * tracking session.</p>
 */
public final class ScentPuffOverlay {

    private static final long PULSE_DURATION_MS = 2000L;
    private static final long FADE_IN_MS = 90L;
    private static final long FADE_OUT_MS = 800L;
    private static final float MIN_VISIBLE_ALPHA = 0.28f;

    private static final int SHEET_FRAME_W = 160;
    private static final int SHEET_FRAME_H = 90;
    private static final long SHEET_FRAME_MS = 120L;

    private static boolean initialized = false;

    private static Identifier activeMask = null;
    private static long pulseStartMs = 0L;
    private static double lastPuffIntensity = 0.5;
    private static String categoryColor;
    private static boolean activeMaskLoops = true;

    private ScentPuffOverlay() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Draw the scent mask BEFORE the HUD so the border/vignette sits behind
        // the hotbar, health, hunger, etc. instead of covering them. Returning
        // true lets the vanilla GUI render proceed (returning false would cancel
        // the entire HUD render).
        RenderCallback.Gui.BEFORE.register((graphics, window) -> {
            render(graphics);
            return true;
        });
        AromaAffect.LOGGER.info("ScentPuffOverlay initialized");
    }

    /**
     * Triggers the scent overlay for a general puff (Omara Device, etc.).
     *
     * @param scentName the scent name (used to resolve the mask texture)
     * @param intensity the scent intensity (0.0 to 1.0)
     */
    public static void onScentPuff(String scentName, double intensity) {
        onScentPuff(scentName, null, intensity);
    }

    /**
     * Triggers the scent overlay, preferring an animated frame registered for the event.
     *
     * @param scentName the scent name (used to resolve the static mask)
     * @param intensity the scent intensity (0.0 to 1.0)
     * @param visualCategory   the firing event id, or null when the puff has no event behind it
     */
    public static void onScentPuff(String scentName, String visualCategory, double intensity) {
        if (scentName == null || scentName.isBlank()) {
            return;
        }

        Identifier mask = resolveMask(scentName, visualCategory);
        if (mask == null) {
            AromaAffect.LOGGER.debug("No mask mapping for scent '{}' (ScentPuffOverlay)", scentName);
            return;
        }

        activeMask = mask;
        pulseStartMs = System.currentTimeMillis();
        lastPuffIntensity = clamp01(intensity);
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (activeMask == null || mc.player == null || mc.isPaused() || false) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - pulseStartMs;
        if (elapsed < 0 || elapsed > PULSE_DURATION_MS) {
            return;
        }

        float pulseAlpha = computePulseAlpha(elapsed);
        if (pulseAlpha <= 0.001f) {
            return;
        }

        float intensityAlpha = (float) (0.75 + lastPuffIntensity * 0.25);
        float rawAlpha = pulseAlpha * intensityAlpha;
        float finalAlpha = clamp01f(Math.max(MIN_VISIBLE_ALPHA * pulseAlpha, rawAlpha));
        if (finalAlpha <= 0.01f) {
            return;
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int tint = ARGB.color(finalAlpha, ColorHelper.getColorAsInt(categoryColor));
        int frames = Math.max(1, ResourceUtil.getTextureHeight(activeMask) / SHEET_FRAME_H);
        long step = elapsed / SHEET_FRAME_MS;
        // A one-shot sheet stops on its last frame rather than snapping back to the
        // start; the puff is fading out by then anyway.
        int frame = activeMaskLoops
                ? (int) (step % frames)
                : (int) Math.min(step, frames - 1L);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                activeMask,
                0,
                0,
                0.0f,
                (frame * SHEET_FRAME_H),
                width,
                height,
                SHEET_FRAME_W,
                SHEET_FRAME_H,
                SHEET_FRAME_W,
                SHEET_FRAME_H * frames,
                tint
        );
    }

    private static float computePulseAlpha(long elapsedMs) {
        if (elapsedMs < FADE_IN_MS) {
            return clamp01f((float) elapsedMs / FADE_IN_MS);
        }
        long fadeOutStart = PULSE_DURATION_MS - FADE_OUT_MS;
        if (elapsedMs >= fadeOutStart) {
            return clamp01f((float) (PULSE_DURATION_MS - elapsedMs) / FADE_OUT_MS);
        }
        return 1.0f;
    }

    private static Identifier resolveMask(String scentName, String categoryID) {
        CategoryDefinition category = CategoryDefinitionLoader.getCategoryFromID(categoryID);

        if (category == null) {
            categoryColor = "#FFFFFF";
            activeMaskLoops = true;
            return resolveScentMask(scentName);
        }

        categoryColor = category.getColorHtml();
        if (category.getMask() == null || category.getMask().isBlank()) {
            activeMaskLoops = true;
            return resolveScentMask(scentName);
        }

        activeMaskLoops = category.isMaskLooping();

        return Identifier.tryParse(category.getMask() + ".png");
    }

    static Identifier resolveScentMask(String scentName) {
        return ScentRegistry.getScentByName(scentName)
                .map(ScentDefinition::getMask)
                .filter(mask -> !mask.isBlank())
                .map(mask -> Identifier.tryParse(mask + ".png"))
                .orElse(null);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static float clamp01f(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
