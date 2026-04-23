package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.util.Colors;
import com.ovrtechnology.util.Texts;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public final class TrackingHud {

    private static boolean initialized = false;

    private static boolean arrivedSoundPlayed = false;

    private static long notificationStart = 0;

    private static ActiveTrackingState.TrackingStatus lastNotifiedStatus =
            ActiveTrackingState.TrackingStatus.IDLE;

    private static final long NOTIFICATION_DURATION_MS = 3000;

    private static final long FADE_OUT_MS = 600;

    private TrackingHud() {}

    public static void init() {
        if (initialized) {
            return;
        }

        ClientTickEvent.CLIENT_POST.register(
                instance -> {
                    ActiveTrackingState.tick();
                    tickNotification();
                });

        ClientGuiEvent.RENDER_HUD.register(
                (graphics, tickDelta) -> {
                    render(graphics);
                });

        initialized = true;
        AromaAffect.LOGGER.info("TrackingHud initialized");
    }

    private static void tickNotification() {
        ActiveTrackingState.TrackingStatus status = ActiveTrackingState.getStatus();

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

        float alpha;
        if (elapsed < 200) {
            alpha = elapsed / 200f;
        } else if (elapsed > NOTIFICATION_DURATION_MS - FADE_OUT_MS) {
            alpha = (float) (NOTIFICATION_DURATION_MS - elapsed) / FADE_OUT_MS;
        } else {
            alpha = 1f;
        }
        alpha = Mth.clamp(alpha, 0f, 1f);
        int a = (int) (alpha * 255) & 0xFF;
        if (a < 4) return;

        String text;
        int accentColor;
        switch (status) {
            case ARRIVED -> {
                text = Texts.tr("tracking.aromaaffect.status.arrived").getString();
                accentColor = Colors.SUCCESS_GREEN_RGB;
            }
            case NOT_FOUND -> {
                text = Texts.tr("tracking.aromaaffect.status.not_found").getString();
                accentColor = 0xFF6644;
            }
            case ERROR -> {
                text = Texts.tr("tracking.aromaaffect.status.error").getString();
                accentColor = Colors.ERROR_RED_RGB;
            }
            default -> {
                return;
            }
        }

        Component displayName = ActiveTrackingState.getDisplayName();
        String targetText = displayName != null ? displayName.getString() : "";

        int screenW = mc.getWindow().getGuiScaledWidth();
        var font = mc.font;

        int textW = Math.max(font.width(text), font.width(targetText));
        int boxW = textW + 20;
        int boxH = targetText.isEmpty() ? 24 : 36;
        int boxX = (screenW - boxW) / 2;
        int boxY = 10;

        int bgColor = (a * 3 / 4) << 24 | 0x111111;
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, bgColor);

        int barColor = (a << 24) | accentColor;
        graphics.fill(boxX, boxY, boxX + 3, boxY + boxH, barColor);

        int textColor = (a << 24) | accentColor;
        int textX = boxX + 10;
        graphics.drawString(font, text, textX, boxY + 6, textColor, false);

        if (!targetText.isEmpty()) {
            int nameColor = (a << 24) | 0xCCCCCC;
            graphics.drawString(font, targetText, textX, boxY + 20, nameColor, false);
        }
    }

    private static void renderPersistentTrackingToast(
            GuiGraphics graphics, Minecraft mc, ActiveTrackingState.TrackingStatus status) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int panelTop = 4;
        int panelRight = screenW - 4;
        int pad = 6;
        int iconSpace = 20;

        int accentArgb =
                (status == ActiveTrackingState.TrackingStatus.SEARCHING)
                        ? Colors.WARNING_YELLOW_ALPHA
                        : Colors.TRACK_GREEN_BUTTON_STRONG;
        int borderArgb =
                (status == ActiveTrackingState.TrackingStatus.SEARCHING)
                        ? Colors.TRACK_YELLOW_ALPHA
                        : Colors.TRACK_GREEN_BUTTON;

        String headerText;
        if (status == ActiveTrackingState.TrackingStatus.SEARCHING) {
            headerText = Texts.tr("tracking.aromaaffect.status.searching").getString();
        } else {
            MenuCategory cat = ActiveTrackingState.getCategory();
            headerText = Texts.tr("menu.aromaaffect.tracking.label").getString();
            if (cat != null) {
                headerText += " · " + cat.getDisplayName().getString();
            }
        }

        Component targetName = ActiveTrackingState.getDisplayName();
        String targetIdStr =
                ActiveTrackingState.getTargetId() != null
                        ? ActiveTrackingState.getTargetId().toString()
                        : null;
        int dist = ActiveTrackingState.getDistance();
        String distText =
                (status == ActiveTrackingState.TrackingStatus.TRACKING && dist >= 0)
                        ? dist + " blocks away"
                        : null;
        TrackingDirectionIndicator.Kind dirKind =
                (status == ActiveTrackingState.TrackingStatus.TRACKING && dist >= 0)
                        ? TrackingDirectionIndicator.resolve(
                                mc, ActiveTrackingState.getDestination())
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
            maxText =
                    Math.max(
                            maxText,
                            TrackingDirectionIndicator.getColumnWidth() + font.width(distText));
        }

        int lineCount = 1;
        if (targetName != null) lineCount++;
        if (targetIdStr != null && status == ActiveTrackingState.TrackingStatus.TRACKING)
            lineCount++;
        if (distText != null) lineCount++;

        int panelWidth = maxText + iconSpace + pad * 2;
        int panelHeight = 10 + lineCount * 11;
        int panelLeft = panelRight - panelWidth;

        graphics.fill(
                panelLeft, panelTop, panelRight, panelTop + panelHeight, Colors.BG_MENU_BACKDROP);
        MenuRenderUtils.renderOutline(
                graphics, panelLeft, panelTop, panelWidth, panelHeight, borderArgb);
        graphics.fill(panelLeft, panelTop, panelLeft + 2, panelTop + panelHeight, accentArgb);

        int iconX = panelLeft + pad + 1;
        int iconY = panelTop + (panelHeight - 16) / 2;
        if (ActiveTrackingState.getIcon() != null) {
            graphics.renderItem(ActiveTrackingState.getIcon(), iconX, iconY);
        }

        int textX = iconX + iconSpace;
        int currentY = panelTop + 5;

        int labelColor =
                (status == ActiveTrackingState.TrackingStatus.SEARCHING)
                        ? Colors.WARNING_YELLOW
                        : Colors.SUCCESS_GREEN_SOFT;
        graphics.drawString(font, headerText, textX, currentY, labelColor);
        currentY += 11;

        if (targetName != null) {
            graphics.drawString(font, targetName, textX, currentY, Colors.WHITE);
            currentY += 11;
        }
        if (targetIdStr != null && status == ActiveTrackingState.TrackingStatus.TRACKING) {
            graphics.drawString(font, targetIdStr, textX, currentY, Colors.TEXT_SUBTLE);
            currentY += 11;
        }
        if (distText != null && dirKind != null) {
            drawDistanceLine(graphics, textX, currentY, dirKind, distText, Colors.ACCENT_CYAN);
        }
    }

    private static void drawDistanceLine(
            GuiGraphics graphics,
            int x,
            int y,
            TrackingDirectionIndicator.Kind directionKind,
            String distanceText,
            int color) {
        Minecraft mc = Minecraft.getInstance();
        int indicatorColor = TrackingDirectionIndicator.colorForKind(directionKind, color);
        TrackingDirectionIndicator.draw(graphics, x, y, directionKind, indicatorColor);
        graphics.drawString(
                mc.font,
                distanceText,
                x + TrackingDirectionIndicator.getColumnWidth(),
                y,
                indicatorColor,
                false);
    }
}
