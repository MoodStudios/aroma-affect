package com.ovrtechnology.trigger.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.menu.ActiveTrackingState;
import com.ovrtechnology.util.Ids;
import dev.architectury.event.events.client.ClientGuiEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

public final class PathTrackingMaskOverlay {

    private static final Map<String, ResourceLocation> SCENT_MASKS = new HashMap<>();

    private static final long PULSE_DURATION_MS = 2800L;
    private static final long FADE_IN_MS = 90L;
    private static final long FADE_OUT_MS = 1400L;
    private static final float MIN_VISIBLE_ALPHA = 0.28f;

    private static boolean initialized = false;

    private static ResourceLocation activeMask = null;
    private static long pulseStartMs = 0L;
    private static double lastPuffIntensity = 0.5;

    static {
        register("winter", "winterlayermask");
        register("barnyard", "barnyardlayermask");
        register("sweet", "sweetlayermask");
        register("floral", "flowerlayermask");
        register("beach", "beachlayermask");
        register("kindred", "kindredlayermask");
        register("petrichor", "rainlayermask");
        register("marine", "marinelayermask");
        register("evergreen", "forestlayermask");
        register("terra silva", "terrasilvalayermask");
        register("citrus", "citruslayermask");
        register("desert", "desertlayermask");
        register("savory spice", "savoryspicelayermask");
        register("timber", "timberlayermask");
        register("smoky", "smokylayermask");
        register("machina", "diesellayermask");
    }

    private PathTrackingMaskOverlay() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> render(graphics));
        AromaAffect.LOGGER.info("PathTrackingMaskOverlay initialized");
    }

    public static void onPathScentPuff(String scentName, double intensity) {
        if (scentName == null || scentName.isBlank()) {
            return;
        }
        ResourceLocation mask = resolveMask(scentName);
        if (mask == null) {
            AromaAffect.LOGGER.debug("No mask mapping for scent '{}'", scentName);
            return;
        }

        activeMask = mask;
        pulseStartMs = System.currentTimeMillis();
        lastPuffIntensity = clamp01(intensity);
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (activeMask == null || mc.player == null || mc.isPaused() || mc.options.hideGui) {
            return;
        }

        ActiveTrackingState.TrackingStatus status = ActiveTrackingState.getStatus();
        if (status != ActiveTrackingState.TrackingStatus.SEARCHING
                && status != ActiveTrackingState.TrackingStatus.TRACKING) {
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

        float distanceAlpha = computeDistanceAlpha(ActiveTrackingState.getDistance());
        float intensityAlpha = (float) (0.75 + lastPuffIntensity * 0.25);
        float rawAlpha = pulseAlpha * distanceAlpha * intensityAlpha;
        float finalAlpha = clamp01f(Math.max(MIN_VISIBLE_ALPHA * pulseAlpha, rawAlpha));
        if (finalAlpha <= 0.01f) {
            return;
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int tint = ARGB.color(finalAlpha, 0xFFFFFF);
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
                tint);
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

    private static float computeDistanceAlpha(int distance) {
        if (distance < 0) return 0.70f;
        if (distance <= 12) return 1.00f;
        if (distance <= 32) return 0.92f;
        if (distance <= 64) return 0.86f;
        if (distance <= 128) return 0.78f;
        if (distance <= 256) return 0.70f;
        return 0.62f;
    }

    private static ResourceLocation resolveMask(String scentName) {
        String key = scentName.toLowerCase(Locale.ROOT).trim();
        return SCENT_MASKS.get(key);
    }

    private static void register(String scentName, String maskFileStem) {
        SCENT_MASKS.put(
                scentName.toLowerCase(Locale.ROOT),
                Ids.mod("textures/masks/" + maskFileStem + ".png"));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static float clamp01f(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
