package com.ovrtechnology.trigger.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.category.CategoryDefinition;
import com.ovrtechnology.category.CategoryDefinitionLoader;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.util.ColorHelper;
import net.blay09.mods.balm.client.platform.event.callback.RenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fullscreen scent mask overlay for non-tracking puffs (Omara Device, etc.).
 *
 * <p>Shows the scent's border/corner image for ~2 seconds with fade-in/fade-out.
 * This is independent of the path tracking system and does not require an active
 * tracking session.</p>
 */
public final class ScentPuffOverlay {

    private static final Map<String, Identifier> SCENT_MASKS = new HashMap<>();

    /**
     * Per-event animated frames, keyed by event id. These override the scent's static
     * mask without changing which scent fires: a golden apple still triggers citrus and
     * redstone still triggers machina, they just get their own border.
     */
    private static final Map<String, Identifier> EVENT_FRAMES = new HashMap<>();

    private static final long PULSE_DURATION_MS = 2000L;
    private static final long FADE_IN_MS = 90L;
    private static final long FADE_OUT_MS = 800L;
    private static final float MIN_VISIBLE_ALPHA = 0.28f;

    /** Animated sheets are a horizontal strip of {@value #SHEET_FRAMES} frames. */
    private static final int SHEET_FRAMES = 5;
    private static final int SHEET_FRAME_W = 160;
    private static final int SHEET_FRAME_H = 90;
    private static final long SHEET_FRAME_MS = 120L;

    private static boolean initialized = false;

    private static Identifier activeMask = null;
    private static boolean activeMaskIsSheet = false;
    private static long pulseStartMs = 0L;
    private static double lastPuffIntensity = 0.5;
    private static String categoryColor;

//    static {
//        registerEventFrame("aromaaffect:player_food_citrus", "golden");
//        registerEventFrame("aromaaffect:block_break_redstone_ore", "redstone_technology");
//        registerEventFrame("aromaaffect:item_crafted_redstone", "redstone_technology");
//    }

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

//        Identifier sheet = eventId != null ? EVENT_FRAMES.get(eventId) : null;
//
//        if (sheet == null) {
//            if (scentName == null || scentName.isBlank()) {
//                return;
//            }
//            Identifier mask = resolveMask(scentName);
//            if (mask == null) {
//                AromaAffect.LOGGER.debug("No mask mapping for scent '{}' (ScentPuffOverlay)", scentName);
//                return;
//            }
//            activeMask = mask;
//            activeMaskIsSheet = false;
//        } else {
//            activeMask = sheet;
//            activeMaskIsSheet = true;
//        }

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

//        if (activeMaskIsSheet) {
//            int frame = (int) ((elapsed / SHEET_FRAME_MS) % SHEET_FRAMES);
//            graphics.blit(
//                    RenderPipelines.GUI_TEXTURED,
//                    activeMask,
//                    0,
//                    0,
//                    (float) (frame * SHEET_FRAME_W),
//                    0.0f,
//                    width,
//                    height,
//                    SHEET_FRAME_W,
//                    SHEET_FRAME_H,
//                    SHEET_FRAME_W * SHEET_FRAMES,
//                    SHEET_FRAME_H,
//                    tint
//            );
//            return;
//        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                activeMask,
                0,
                0,
                0.0f,
                0.0f,
                width,
                height,
                width,
                height,
                width,
                height,
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

        if (category != null) {
            if (category.getColorHtml() != null) {
                categoryColor = category.getColorHtml();
            }

            if (category.getMask() == null) {
                return getScentMask(scentName);
            }

            return Identifier.parse(category.getMask() + ".png");
        }

        categoryColor = "#FFFFFF";
        return getScentMask(scentName);
    }

    private static Identifier getScentMask(String scentName) {
        var scentDef = ScentRegistry.getScentByName(scentName.toLowerCase());
        ScentDefinition scent;

        if (scentDef.isPresent()) {
            scent = scentDef.get();
            return Identifier.parse(scent.getMask() + ".png");
        }

        return null;
    }

//    private static void registerEventFrame(String eventId, String sheetFileStem) {
//        EVENT_FRAMES.put(
//                eventId,
//                Identifier.fromNamespaceAndPath(
//                        AromaAffect.MOD_ID, "textures/masks/animated/" + sheetFileStem + ".png")
//        );
//    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static float clamp01f(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
