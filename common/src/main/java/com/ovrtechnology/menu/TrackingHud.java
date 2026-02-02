package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
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
}
