package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import com.ovrtechnology.trigger.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * HUD overlay that shows tracking status notifications (Arrived!, Not Found, etc.)
 * even when the radial menu is not open. Also ticks {@link ActiveTrackingState}
 * globally so auto-clear works regardless of which screen is open.
 */
public final class TrackingHud {

    private static boolean initialized = false;

    /** Whether a sound has already been played for the current ARRIVED state. */
    private static boolean arrivedSoundPlayed = false;

    /** Timestamp when the current notification started (for fade animation). */
    private static long notificationStart = 0;

    /** The status we're currently showing a notification for. */
    private static ActiveTrackingState.TrackingStatus lastNotifiedStatus = ActiveTrackingState.TrackingStatus.IDLE;

    /** How long the notification is visible (matches auto-clear). */
    private static final long NOTIFICATION_DURATION_MS = 3000;

    /** Fade-out starts this many ms before the notification ends. */
    private static final long FADE_OUT_MS = 600;

    private TrackingHud() {}

    public static void init() {
        if (initialized) {
            return;
        }

        // Tick ActiveTrackingState globally
        ClientTickEvent.CLIENT_POST.register(instance -> {
            ActiveTrackingState.tick();
            tickNotification();
        });

        // Render HUD overlay
        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            render(graphics);
        });

        initialized = true;
        AromaAffect.LOGGER.info("TrackingHud initialized");
    }

    private static void tickNotification() {
        ActiveTrackingState.TrackingStatus status = ActiveTrackingState.getStatus();

        // Detect new terminal state
        if (status == ActiveTrackingState.TrackingStatus.ARRIVED
                || status == ActiveTrackingState.TrackingStatus.NOT_FOUND
                || status == ActiveTrackingState.TrackingStatus.ERROR) {

            if (status != lastNotifiedStatus) {
                lastNotifiedStatus = status;
                notificationStart = System.currentTimeMillis();

                if (status == ActiveTrackingState.TrackingStatus.ARRIVED && !arrivedSoundPlayed) {
                    playArrivedSound();
                    arrivedSoundPlayed = true;
                }
            }
        } else {
            // Reset when back to idle/searching/tracking
            if (lastNotifiedStatus != status) {
                lastNotifiedStatus = status;
                arrivedSoundPlayed = false;
            }
        }
    }

    private static void playArrivedSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.7f, 1.4f);
        }
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null || mc.options.hideGui) {
            return;
        }

        // Don't render if radial menu (or any BaseMenuScreen) is open — it has its own panel
        if (mc.screen instanceof BaseMenuScreen) {
            return;
        }

        ActiveTrackingState.TrackingStatus status = ActiveTrackingState.getStatus();
        if (ClientConfig.getInstance().isTrackingToastPersistent()
                && (status == ActiveTrackingState.TrackingStatus.SEARCHING
                || status == ActiveTrackingState.TrackingStatus.TRACKING)) {
            renderPersistentTrackingToast(graphics, mc, status);
        }

        if (status != ActiveTrackingState.TrackingStatus.ARRIVED
                && status != ActiveTrackingState.TrackingStatus.NOT_FOUND
                && status != ActiveTrackingState.TrackingStatus.ERROR) {
            return;
        }

        long elapsed = System.currentTimeMillis() - notificationStart;
        if (elapsed > NOTIFICATION_DURATION_MS) {
            return;
        }

        // Calculate alpha with fade-in and fade-out
        float alpha;
        if (elapsed < 200) {
            alpha = elapsed / 200f; // fade in
        } else if (elapsed > NOTIFICATION_DURATION_MS - FADE_OUT_MS) {
            alpha = (float) (NOTIFICATION_DURATION_MS - elapsed) / FADE_OUT_MS;
        } else {
            alpha = 1f;
        }
        alpha = Mth.clamp(alpha, 0f, 1f);
        int a = (int) (alpha * 255) & 0xFF;
        if (a < 4) return;

        // Determine text and color
        String text;
        int accentColor;
        switch (status) {
            case ARRIVED -> {
                text = Component.translatable("tracking.aromaaffect.status.arrived").getString();
                accentColor = 0x44FF44;
            }
            case NOT_FOUND -> {
                text = Component.translatable("tracking.aromaaffect.status.not_found").getString();
                accentColor = 0xFF6644;
            }
            case ERROR -> {
                text = Component.translatable("tracking.aromaaffect.status.error").getString();
                accentColor = 0xFF4444;
            }
            default -> { return; }
        }

        // Display name of target
        Component displayName = ActiveTrackingState.getDisplayName();
        String targetText = displayName != null ? displayName.getString() : "";

        int screenW = mc.getWindow().getGuiScaledWidth();
        var font = mc.font;

        int textW = Math.max(font.width(text), font.width(targetText));
        int boxW = textW + 20;
        int boxH = targetText.isEmpty() ? 24 : 36;
        int boxX = (screenW - boxW) / 2;
        int boxY = 10;

        // Background
        int bgColor = (a * 3 / 4) << 24 | 0x111111;
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, bgColor);

        // Accent bar on left
        int barColor = (a << 24) | accentColor;
        graphics.fill(boxX, boxY, boxX + 3, boxY + boxH, barColor);

        // Main text
        int textColor = (a << 24) | accentColor;
        int textX = boxX + 10;
        graphics.drawString(font, text, textX, boxY + 6, textColor, false);

        // Target name (smaller, white)
        if (!targetText.isEmpty()) {
            int nameColor = (a << 24) | 0xCCCCCC;
            graphics.drawString(font, targetText, textX, boxY + 20, nameColor, false);
        }
    }

    private static void renderPersistentTrackingToast(
            GuiGraphics graphics,
            Minecraft mc,
            ActiveTrackingState.TrackingStatus status
    ) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int panelTop = 4;
        int panelRight = screenW - 4;
        int pad = 6;
        int iconSpace = 20;

        int accentArgb = (status == ActiveTrackingState.TrackingStatus.SEARCHING) ? 0xDDFFCC44 : 0xDD44FF44;
        int borderArgb = (status == ActiveTrackingState.TrackingStatus.SEARCHING) ? 0xAAAA8833 : 0xAA44AA44;

        String headerText;
        if (status == ActiveTrackingState.TrackingStatus.SEARCHING) {
            headerText = Component.translatable("tracking.aromaaffect.status.searching").getString();
        } else {
            MenuCategory cat = ActiveTrackingState.getCategory();
            headerText = Component.translatable("menu.aromaaffect.tracking.label").getString();
            if (cat != null) {
                headerText += " · " + cat.getDisplayName().getString();
            }
        }

        Component targetName = ActiveTrackingState.getDisplayName();
        String targetIdStr = ActiveTrackingState.getTargetId() != null ? ActiveTrackingState.getTargetId().toString() : null;
        int dist = ActiveTrackingState.getDistance();
        String distText = (status == ActiveTrackingState.TrackingStatus.TRACKING && dist >= 0)
                ? dist + " blocks away"
                : null;
        TrackingDirectionIndicator.Kind dirKind = (status == ActiveTrackingState.TrackingStatus.TRACKING && dist >= 0)
                ? TrackingDirectionIndicator.resolve(mc, ActiveTrackingState.getDestination())
                : null;

        var font = mc.font;
        int maxText = font.width(headerText);
        if (targetName != null) {
            maxText = Math.max(maxText, font.width(targetName));
        }
        if (targetIdStr != null && status == ActiveTrackingState.TrackingStatus.TRACKING) {
            maxText = Math.max(maxText, font.width(targetIdStr));
        }
        if (distText != null) {
            maxText = Math.max(maxText, TrackingDirectionIndicator.getColumnWidth() + font.width(distText));
        }

        int lineCount = 1;
        if (targetName != null) lineCount++;
        if (targetIdStr != null && status == ActiveTrackingState.TrackingStatus.TRACKING) lineCount++;
        if (distText != null) lineCount++;

        int panelWidth = maxText + iconSpace + pad * 2;
        int panelHeight = 10 + lineCount * 11;
        int panelLeft = panelRight - panelWidth;

        graphics.fill(panelLeft, panelTop, panelRight, panelTop + panelHeight, 0xDD1A1A2E);
        MenuRenderUtils.renderOutline(graphics, panelLeft, panelTop, panelWidth, panelHeight, borderArgb);
        graphics.fill(panelLeft, panelTop, panelLeft + 2, panelTop + panelHeight, accentArgb);

        int iconX = panelLeft + pad + 1;
        int iconY = panelTop + (panelHeight - 16) / 2;
        if (ActiveTrackingState.getIcon() != null) {
            graphics.renderItem(ActiveTrackingState.getIcon(), iconX, iconY);
        }

        int textX = iconX + iconSpace;
        int currentY = panelTop + 5;

        int labelColor = (status == ActiveTrackingState.TrackingStatus.SEARCHING) ? 0xFFFFCC44 : 0xFF88CC88;
        graphics.drawString(font, headerText, textX, currentY, labelColor);
        currentY += 11;

        if (targetName != null) {
            graphics.drawString(font, targetName, textX, currentY, 0xFFFFFFFF);
            currentY += 11;
        }
        if (targetIdStr != null && status == ActiveTrackingState.TrackingStatus.TRACKING) {
            graphics.drawString(font, targetIdStr, textX, currentY, 0xFF777777);
            currentY += 11;
        }
        if (distText != null && dirKind != null) {
            drawDistanceLine(graphics, textX, currentY, dirKind, distText, 0xFF44CCFF);
        }
    }

    private static void drawDistanceLine(
            GuiGraphics graphics,
            int x,
            int y,
            TrackingDirectionIndicator.Kind directionKind,
            String distanceText,
            int color
    ) {
        Minecraft mc = Minecraft.getInstance();
        int indicatorColor = TrackingDirectionIndicator.colorForKind(directionKind, color);
        TrackingDirectionIndicator.draw(graphics, x, y, directionKind, indicatorColor);
        graphics.drawString(mc.font, distanceText, x + TrackingDirectionIndicator.getColumnWidth(), y, indicatorColor, false);
    }
}
