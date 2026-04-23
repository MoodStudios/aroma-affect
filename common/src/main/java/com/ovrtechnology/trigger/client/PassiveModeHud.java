package com.ovrtechnology.trigger.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.PassiveModeManager;
import com.ovrtechnology.trigger.ScentTrigger;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.ScentTriggerSource;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.TriggerSettings;
import com.ovrtechnology.util.Colors;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class PassiveModeHud {

    private static final int MARGIN = 8;

    private static final int CIRCLE_RADIUS = 6;

    private static final int CIRCLE_THICKNESS = 2;
    private static final int CIRCLE_SPACING = 4;

    private static final int CIRCLE_SEGMENTS = 32;

    private static final int COLOR_BACKGROUND = Colors.OVERLAY_DARK_STRONG;

    private static final int COLOR_READY = 0xFF00CC66;
    private static final int COLOR_COOLDOWN = 0xFFCC6600;
    private static final int COLOR_GLOBAL_COOLDOWN = 0xFFCC3366;
    private static final int COLOR_BORDER = Colors.BG_DARK_PANEL;

    private static boolean initialized = false;

    private PassiveModeHud() {}

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("PassiveModeHud.init() called multiple times!");
            return;
        }

        ClientGuiEvent.RENDER_HUD.register(
                (graphics, tickDelta) -> {
                    render(graphics);
                });

        initialized = true;
        AromaAffect.LOGGER.info("PassiveModeHud initialized");
    }

    private static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.isPaused() || minecraft.player == null) {
            return;
        }

        if (minecraft.options.hideGui) {
            return;
        }

        ScentTriggerManager manager = ScentTriggerManager.getInstance();
        ScentTrigger activeScent = manager.getActiveScent();
        ScentTrigger displayScent =
                activeScent != null ? activeScent : manager.getLastTriggeredScent();

        if (displayScent == null || (!manager.hasActiveScent() && !manager.hasActiveCooldown())) {
            return;
        }

        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int centerY = screenHeight - MARGIN - CIRCLE_RADIUS;

        int globalCenterX = MARGIN + CIRCLE_RADIUS;
        float globalProgress = calculateGlobalCooldownProgress(displayScent);
        renderCircularIndicator(
                graphics, globalCenterX, centerY, globalProgress, COLOR_GLOBAL_COOLDOWN);

        int scentCenterX = globalCenterX + (CIRCLE_RADIUS * 2) + CIRCLE_SPACING;
        float scentProgress = calculateScentCooldownProgress(displayScent);
        renderCircularIndicator(graphics, scentCenterX, centerY, scentProgress, COLOR_COOLDOWN);
    }

    private static float calculateGlobalCooldownProgress(ScentTrigger activeScent) {
        long cooldownMs = ClientConfig.getInstance().getGlobalCooldownMs();
        if (cooldownMs <= 0) {
            cooldownMs = TriggerSettings.DEFAULT_GLOBAL_COOLDOWN_MS;
        }

        long lastTriggerTime = ScentTriggerManager.getInstance().getLastGlobalTriggerTime();
        long now = System.currentTimeMillis();
        long elapsedMs = now - lastTriggerTime;
        long remainingMs = Math.max(0, cooldownMs - elapsedMs);

        float progress = (float) remainingMs / cooldownMs;
        return Math.max(0f, Math.min(1f, progress));
    }

    private static float calculateScentCooldownProgress(ScentTrigger activeScent) {

        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        long cooldownMs;

        if (activeScent.source() == ScentTriggerSource.PASSIVE_MODE) {

            cooldownMs = PassiveModeManager.getCurrentCooldownMs();
            if (cooldownMs <= 0) {

                cooldownMs = settings.getBiomeCooldownMs();
            }
        } else if (activeScent.source() == ScentTriggerSource.ITEM_USE) {
            cooldownMs = settings.getItemUseCooldownMs();
        } else {

            cooldownMs = settings.getScentCooldownMs();
        }

        if (cooldownMs <= 0) {
            cooldownMs = 5000;
        }

        long lastTriggerTime =
                ScentTriggerManager.getInstance().getLastTriggerTime(activeScent.scentName());
        long now = System.currentTimeMillis();
        long elapsedMs = now - lastTriggerTime;
        long remainingMs = Math.max(0, cooldownMs - elapsedMs);

        float progress = (float) remainingMs / cooldownMs;
        return Math.max(0f, Math.min(1f, progress));
    }

    private static void renderCircularIndicator(
            GuiGraphics graphics, int centerX, int centerY, float progress, int cooldownColor) {
        int outerRadius = CIRCLE_RADIUS;
        int innerRadius = CIRCLE_RADIUS - CIRCLE_THICKNESS;

        drawCircleRing(
                graphics, centerX, centerY, outerRadius, innerRadius, 0f, 1f, COLOR_BACKGROUND);

        drawCircleRing(
                graphics, centerX, centerY, outerRadius + 1, outerRadius, 0f, 1f, COLOR_BORDER);

        if (progress > 0) {

            float fillProgress = 1f - progress;
            drawCircleRing(
                    graphics,
                    centerX,
                    centerY,
                    outerRadius,
                    innerRadius,
                    0f,
                    fillProgress,
                    cooldownColor);
        } else {

            drawCircleRing(
                    graphics, centerX, centerY, outerRadius, innerRadius, 0f, 1f, COLOR_READY);
        }
    }

    private static void drawCircleRing(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int outerRadius,
            int innerRadius,
            float startFrac,
            float endFrac,
            int color) {
        if (endFrac <= startFrac) {
            return;
        }

        double startAngle = -Math.PI / 2 + (startFrac * 2 * Math.PI);
        double endAngle = -Math.PI / 2 + (endFrac * 2 * Math.PI);
        double angleStep = (endAngle - startAngle) / CIRCLE_SEGMENTS;

        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double angle1 = startAngle + i * angleStep;
            double angle2 = startAngle + (i + 1) * angleStep;

            int x1Outer = centerX + (int) (Math.cos(angle1) * outerRadius);
            int y1Outer = centerY + (int) (Math.sin(angle1) * outerRadius);
            int x2Outer = centerX + (int) (Math.cos(angle2) * outerRadius);
            int y2Outer = centerY + (int) (Math.sin(angle2) * outerRadius);

            int x1Inner = centerX + (int) (Math.cos(angle1) * innerRadius);
            int y1Inner = centerY + (int) (Math.sin(angle1) * innerRadius);
            int x2Inner = centerX + (int) (Math.cos(angle2) * innerRadius);
            int y2Inner = centerY + (int) (Math.sin(angle2) * innerRadius);

            drawTriangle(graphics, x1Outer, y1Outer, x2Outer, y2Outer, x1Inner, y1Inner, color);
            drawTriangle(graphics, x2Outer, y2Outer, x2Inner, y2Inner, x1Inner, y1Inner, color);
        }
    }

    private static void drawTriangle(
            GuiGraphics graphics, int x1, int y1, int x2, int y2, int x3, int y3, int color) {

        int minX = Math.min(x1, Math.min(x2, x3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (pointInTriangle(x, y, x1, y1, x2, y2, x3, y3)) {
                    graphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    private static boolean pointInTriangle(
            int px, int py, int x1, int y1, int x2, int y2, int x3, int y3) {
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
