package com.ovrtechnology.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.menu.MenuRenderUtils;
import com.ovrtechnology.menu.ModpackConfig;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.websocket.ConnectionState;
import com.ovrtechnology.websocket.OvrWebSocketClient;
import net.blay09.mods.balm.client.platform.event.callback.ScreenCallback;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.util.FormattedCharSequence;

import java.net.URI;
import java.util.List;

public class ClientHooks {

    private static final Identifier INFO_ICON =
            Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/sprites/status/info.png");

    private static final String OVR_URL = "https://www.ovrtechnology.com/?ref=aromaaffect-mod";
    private static final String DEFAULT_DEVICE_NAME = "Omara";

    private static final int TEXT_COLOR = ARGB.color(1.0f, 0xFFFFFF);
    private static final int LINK_COLOR = ARGB.color(1.0f, 0x6D9EF8);
    private static final int LINK_HOVER_COLOR = ARGB.color(1.0f, 0xAFC8FF);
    private static final int MARGIN = 4;
    private static final int ICON_SIZE = 8;
    private static final int ICON_GAP = 3;
    private static final int TOOLTIP_MAX_WIDTH = 220;

    private static int linkX1, linkY1, linkX2, linkY2;

    public static void init() {
        ScreenCallback.Render.AFTER.register((screen, guiGraphics, mouseX, mouseY, delta) -> {
            if (!(screen instanceof TitleScreen titleScreen)) {
                return;
            }
            if (!ClientConfig.getInstance().isOmaraStatusOverlay()) {
                return;
            }
            renderStatus(titleScreen, guiGraphics, mouseX, mouseY);
        });

        ScreenCallback.MousePress.Before.EVENT.register((screen, event) -> {
            if (!(screen instanceof TitleScreen)) {
                return false;
            }
            if (!ClientConfig.getInstance().isOmaraStatusOverlay() || event.button() != 0) {
                return false;
            }
            if (event.x() >= linkX1 && event.x() < linkX2 && event.y() >= linkY1 && event.y() < linkY2) {
                try {
                    Util.getPlatform().openUri(URI.create(ModpackConfig.appendModpackParam(OVR_URL)));
                    MenuRenderUtils.playClickSound();
                } catch (Exception ignored) {
                }
                return true;
            }
            return false;
        });
    }

    private static void renderStatus(TitleScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Font font = screen.getFont();
        OvrWebSocketClient client = OvrWebSocketClient.getInstance();
        ConnectionState state = client.getState();
        boolean connected = state == ConnectionState.CONNECTED;
        // "Reconnecting" only makes sense after a real connection dropped. Without a device the
        // client never connects, so a fresh launch that only ever failed stays on "Ready".
        // The reconnect loop briefly dips through CONNECTING/CONNECTION_FAILED between each
        // RECONNECTING wait; treat those as "reconnecting" too so the text doesn't flicker.
        boolean reconnecting = client.hasEverConnected()
                && (state == ConnectionState.RECONNECTING
                    || ((state == ConnectionState.CONNECTING || state == ConnectionState.CONNECTION_FAILED)
                        && client.getReconnectAttempts() > 0));
        boolean linked = connected || reconnecting;

        Component prefix;
        if (connected) {
            prefix = Component.translatable("aromaaffect.bridge.connected");
        } else if (reconnecting) {
            prefix = Component.translatable("aromaaffect.bridge.reconnecting");
        } else {
            prefix = Component.translatable("aromaaffect.bridge.ready");
        }

        String deviceName = DEFAULT_DEVICE_NAME;
        if (linked) {
            String reported = client.getDeviceName();
            if (reported != null && !reported.isBlank()) {
                deviceName = reported;
            }
        }
        Component nameToken = Component.literal(deviceName).withStyle(style -> style.withUnderlined(true));

        int x = MARGIN;
        int y = MARGIN;

        graphics.text(font, prefix, x, y, TEXT_COLOR, true);
        int nameX = x + font.width(prefix) + font.width(" ");
        int nameW = font.width(nameToken);

        boolean nameHovered = mouseX >= nameX && mouseX < nameX + nameW && mouseY >= y - 1 && mouseY < y + font.lineHeight;
        graphics.text(font, nameToken, nameX, y, nameHovered ? LINK_HOVER_COLOR : LINK_COLOR, true);

        linkX1 = nameX;
        linkY1 = y - 1;
        linkX2 = nameX + nameW;
        linkY2 = y + font.lineHeight;

        int iconX = nameX + nameW + ICON_GAP;
        graphics.blit(RenderPipelines.GUI_TEXTURED, INFO_ICON,
                iconX, y, 0.0f, 0.0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        boolean infoHovered = mouseX >= nameX && mouseX < iconX + ICON_SIZE && mouseY >= y - 1 && mouseY < y + font.lineHeight;
        if (infoHovered) {
            renderInfoTooltip(font, graphics, mouseX, mouseY, screen.width, screen.height);
        }
    }

    private static void renderInfoTooltip(Font font, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                          int screenW, int screenH) {
        Component info = Component.translatable("aromaaffect.bridge.info");
        List<FormattedCharSequence> lines = font.split(info, TOOLTIP_MAX_WIDTH);

        int textW = 0;
        for (FormattedCharSequence line : lines) {
            textW = Math.max(textW, font.width(line));
        }

        int padding = 5;
        int lineH = font.lineHeight + 1;
        int boxW = textW + padding * 2;
        int boxH = lines.size() * lineH + padding * 2 - 1;

        int boxX = mouseX + 12;
        int boxY = mouseY + 12;
        if (boxX + boxW > screenW - 2) boxX = Math.max(2, screenW - 2 - boxW);
        if (boxY + boxH > screenH - 2) boxY = Math.max(2, mouseY - boxH - 4);

        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xF00A0A12);
        MenuRenderUtils.renderOutline(graphics, boxX, boxY, boxW, boxH, 0x883684F4);

        int ty = boxY + padding;
        for (FormattedCharSequence line : lines) {
            graphics.text(font, line, boxX + padding, ty, 0xFFE8E8E8);
            ty += lineH;
        }
    }
}
