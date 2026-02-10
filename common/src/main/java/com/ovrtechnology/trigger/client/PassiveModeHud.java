package com.ovrtechnology.trigger.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.PassiveModeManager;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.TriggerSettings;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * HUD overlay that displays the current passive mode scent status.
 *
 * <p>Shows two small circular cooldown indicators in the corner:</p>
 * <ul>
 *   <li>Left circle: Global cooldown (applies to all scents)</li>
 *   <li>Right circle: Per-scent cooldown (specific to current scent type)</li>
 * </ul>
 */
public final class PassiveModeHud {

    /**
     * HUD position offset from the corner.
     */
    private static final int MARGIN = 8;

    /**
     * Circular indicator dimensions.
     */
    private static final int CIRCLE_RADIUS = 6;
    private static final int CIRCLE_THICKNESS = 2;
    private static final int CIRCLE_SPACING = 4;

    /**
     * Number of segments for drawing the circle.
     */
    private static final int CIRCLE_SEGMENTS = 32;

    /**
     * Colors (ARGB format).
     */
    private static final int COLOR_BACKGROUND = 0x80000000;
    private static final int COLOR_READY = 0xFF00CC66;
    private static final int COLOR_COOLDOWN = 0xFFCC6600;
    private static final int COLOR_GLOBAL_COOLDOWN = 0xFFCC3366;
    private static final int COLOR_BORDER = 0xFF333333;

    private static boolean initialized = false;

    private PassiveModeHud() {
    }

    /**
     * Initializes the HUD renderer.
     * Should be called during client initialization.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("PassiveModeHud.init() called multiple times!");
            return;
        }

        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            render(graphics);
        });

        initialized = true;
        AromaAffect.LOGGER.info("PassiveModeHud initialized");
    }

    /**
     * Renders the passive mode HUD.
     */
    private static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();

        // Don't render if game is paused or no player
        if (minecraft.isPaused() || minecraft.player == null) {
            return;
        }

        // Don't render if HUD is hidden
        if (minecraft.options.hideGui) {
            return;
        }

        // Get active scent
        ScentTriggerManager manager = ScentTriggerManager.getInstance();
        ScentTrigger activeScent = manager.getActiveScent();

        // Show HUD for any active scent (not just passive mode)
        if (activeScent == null) {
            return;
        }

        // Calculate position (bottom-left corner)
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int centerY = screenHeight - MARGIN - CIRCLE_RADIUS;

        // Left circle: Global cooldown
        int globalCenterX = MARGIN + CIRCLE_RADIUS;
        float globalProgress = calculateGlobalCooldownProgress(activeScent);
        renderCircularIndicator(graphics, globalCenterX, centerY, globalProgress, COLOR_GLOBAL_COOLDOWN);

        // Right circle: Per-scent type cooldown
        int scentCenterX = globalCenterX + (CIRCLE_RADIUS * 2) + CIRCLE_SPACING;
        float scentProgress = calculateScentCooldownProgress(activeScent);
        renderCircularIndicator(graphics, scentCenterX, centerY, scentProgress, COLOR_COOLDOWN);
    }

    /**
     * Calculates the global cooldown progress (0.0 = ready, 1.0 = full cooldown).
     * Global cooldown applies to ALL scents.
     */
    private static float calculateGlobalCooldownProgress(ScentTrigger activeScent) {
        long cooldownMs = ClientConfig.getInstance().getGlobalCooldownMs();
        if (cooldownMs <= 0) {
            cooldownMs = TriggerSettings.DEFAULT_GLOBAL_COOLDOWN_MS;
        }

        // Use the actual last global trigger time from the manager
        long lastTriggerTime = ScentTriggerManager.getInstance().getLastGlobalTriggerTime();
        long now = System.currentTimeMillis();
        long elapsedMs = now - lastTriggerTime;
        long remainingMs = Math.max(0, cooldownMs - elapsedMs);

        float progress = (float) remainingMs / cooldownMs;
        return Math.max(0f, Math.min(1f, progress));
    }

    /**
     * Calculates the per-scent cooldown progress (0.0 = ready, 1.0 = full cooldown).
     * This cooldown is specific to the scent name.
     */
    private static float calculateScentCooldownProgress(ScentTrigger activeScent) {
        // Get the cooldown duration based on trigger source
        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        long cooldownMs;

        // Determine cooldown based on source type
        if (activeScent.source() == ScentTriggerSource.PASSIVE_MODE) {
            // For passive mode, use the type-specific cooldown
            cooldownMs = PassiveModeManager.getCurrentCooldownMs();
            if (cooldownMs <= 0) {
                // Fallback based on biome cooldown as default for passive
                cooldownMs = settings.getBiomeCooldownMs();
            }
        } else if (activeScent.source() == ScentTriggerSource.ITEM_USE) {
            cooldownMs = settings.getItemUseCooldownMs();
        } else {
            // Default fallback
            cooldownMs = settings.getScentCooldownMs();
        }

        if (cooldownMs <= 0) {
            cooldownMs = 5000; // Fallback default
        }

        // Use the actual last trigger time for this specific scent
        long lastTriggerTime = ScentTriggerManager.getInstance().getLastTriggerTime(activeScent.scentName());
        long now = System.currentTimeMillis();
        long elapsedMs = now - lastTriggerTime;
        long remainingMs = Math.max(0, cooldownMs - elapsedMs);

        float progress = (float) remainingMs / cooldownMs;
        return Math.max(0f, Math.min(1f, progress));
    }

    /**
     * Renders a circular cooldown indicator.
     *
     * @param graphics      the graphics context
     * @param centerX       center X position
     * @param centerY       center Y position
     * @param progress      cooldown progress (0.0 = ready, 1.0 = full cooldown)
     * @param cooldownColor color to use when on cooldown
     */
    private static void renderCircularIndicator(GuiGraphics graphics, int centerX, int centerY,
                                                 float progress, int cooldownColor) {
        int outerRadius = CIRCLE_RADIUS;
        int innerRadius = CIRCLE_RADIUS - CIRCLE_THICKNESS;

        // Draw background circle (full ring)
        drawCircleRing(graphics, centerX, centerY, outerRadius, innerRadius, 0f, 1f, COLOR_BACKGROUND);

        // Draw border
        drawCircleRing(graphics, centerX, centerY, outerRadius + 1, outerRadius, 0f, 1f, COLOR_BORDER);

        if (progress > 0) {
            // Cooldown active - draw arc showing progress (fills clockwise from top)
            float fillProgress = 1f - progress;
            drawCircleRing(graphics, centerX, centerY, outerRadius, innerRadius, 0f, fillProgress, cooldownColor);
        } else {
            // Ready - show full green circle
            drawCircleRing(graphics, centerX, centerY, outerRadius, innerRadius, 0f, 1f, COLOR_READY);
        }
    }

    /**
     * Draws a ring (donut) segment using filled triangles.
     *
     * @param graphics    the graphics context
     * @param centerX     center X position
     * @param centerY     center Y position
     * @param outerRadius outer radius
     * @param innerRadius inner radius
     * @param startFrac   start fraction (0.0 = top, clockwise)
     * @param endFrac     end fraction (1.0 = full circle)
     * @param color       ARGB color
     */
    private static void drawCircleRing(GuiGraphics graphics, int centerX, int centerY,
                                        int outerRadius, int innerRadius,
                                        float startFrac, float endFrac, int color) {
        if (endFrac <= startFrac) {
            return;
        }

        // Start angle at top (-90 degrees = -PI/2)
        double startAngle = -Math.PI / 2 + (startFrac * 2 * Math.PI);
        double endAngle = -Math.PI / 2 + (endFrac * 2 * Math.PI);
        double angleStep = (endAngle - startAngle) / CIRCLE_SEGMENTS;

        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double angle1 = startAngle + i * angleStep;
            double angle2 = startAngle + (i + 1) * angleStep;

            // Outer points
            int x1Outer = centerX + (int) (Math.cos(angle1) * outerRadius);
            int y1Outer = centerY + (int) (Math.sin(angle1) * outerRadius);
            int x2Outer = centerX + (int) (Math.cos(angle2) * outerRadius);
            int y2Outer = centerY + (int) (Math.sin(angle2) * outerRadius);

            // Inner points
            int x1Inner = centerX + (int) (Math.cos(angle1) * innerRadius);
            int y1Inner = centerY + (int) (Math.sin(angle1) * innerRadius);
            int x2Inner = centerX + (int) (Math.cos(angle2) * innerRadius);
            int y2Inner = centerY + (int) (Math.sin(angle2) * innerRadius);

            // Draw two triangles to form a quad segment
            drawTriangle(graphics, x1Outer, y1Outer, x2Outer, y2Outer, x1Inner, y1Inner, color);
            drawTriangle(graphics, x2Outer, y2Outer, x2Inner, y2Inner, x1Inner, y1Inner, color);
        }
    }

    /**
     * Draws a filled triangle using small rectangles (approximation).
     * Uses a simple scanline approach for better visual quality.
     */
    private static void drawTriangle(GuiGraphics graphics, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        // Find bounding box
        int minX = Math.min(x1, Math.min(x2, x3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));

        // Simple point-in-triangle fill
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (pointInTriangle(x, y, x1, y1, x2, y2, x3, y3)) {
                    graphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    /**
     * Checks if a point is inside a triangle using barycentric coordinates.
     */
    private static boolean pointInTriangle(int px, int py, int x1, int y1, int x2, int y2, int x3, int y3) {
        int d1 = sign(px, py, x1, y1, x2, y2);
        int d2 = sign(px, py, x2, y2, x3, y3);
        int d3 = sign(px, py, x3, y3, x1, y1);

        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);

        return !(hasNeg && hasPos);
    }

    private static int sign(int px, int py, int x1, int y1, int x2, int y2) {
        return (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2);
    }
}
