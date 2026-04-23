package com.ovrtechnology.omara;

import com.ovrtechnology.menu.MenuRenderUtils;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.scentitem.ScentItem;
import com.ovrtechnology.util.Colors;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.util.Texts;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class OmaraDeviceScreen extends AbstractContainerScreen<OmaraDeviceMenu> {

    private static final ResourceLocation TEXTURE =
            Ids.mod("textures/gui/container/omara_device.png");

    private static final int INFO_X = 98;

    private static final int BAR_W = 40;
    private static final int BAR_H = 5;

    private static final int BTN_X = 8;
    private static final int BTN_W = 66;
    private static final int BTN_H = 12;
    private static final int BTN_INTERVAL_Y = 44;
    private static final int BTN_MODE_Y = 58;

    public OmaraDeviceScreen(OmaraDeviceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        renderInfoPanel(guiGraphics, x, y);
        renderModeButton(guiGraphics, x, y, mouseX, mouseY);
        if (this.menu.getMode() == 0) {
            renderIntervalButton(guiGraphics, x, y, mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            double mx = mouseX;
            double my = mouseY;

            if (isInBounds(mx, my, x + BTN_X, y + BTN_MODE_Y, BTN_W, BTN_H)) {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    MenuRenderUtils.playClickSound();
                }
                return true;
            }

            if (this.menu.getMode() == 0
                    && isInBounds(mx, my, x + BTN_X, y + BTN_INTERVAL_Y, BTN_W, BTN_H)) {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    MenuRenderUtils.playClickSound();
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderInfoPanel(GuiGraphics g, int x, int y) {
        ItemStack capsule = this.menu.getSlot(0).getItem();
        int infoX = x + INFO_X;

        if (capsule.isEmpty()
                || !(capsule.getItem() instanceof ScentItem si)
                || !si.getDefinition().isCapsule()) {
            return;
        }

        String scentName = si.getDefinition().getScent();
        String fallbackName = si.getDefinition().getFallbackName();

        int[] rgb = {255, 255, 255};
        Optional<ScentDefinition> scentOpt = ScentRegistry.getScentByName(scentName);
        if (scentOpt.isPresent()) {
            rgb = scentOpt.get().getColorRGB();
        }
        int scentColor = Colors.BLACK | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];

        int row1Y = y + 24;
        g.fill(infoX, row1Y + 1, infoX + 4, row1Y + 5, scentColor);

        int maxNamePx = (x + this.imageWidth - 4) - (infoX + 6);
        String displayName = trimToWidth(fallbackName, maxNamePx);
        g.drawString(this.font, displayName, infoX + 6, row1Y, Colors.WHITE, true);

        int row2Y = y + 36;
        drawBatteryIcon(g, infoX, row2Y, scentColor);

        int barX = infoX + 8;
        g.fill(barX, row2Y, barX + BAR_W, row2Y + BAR_H, Colors.BG_DARK_PANEL);

        int remaining = capsule.getMaxDamage() - capsule.getDamageValue();
        int maxCharges = capsule.getMaxDamage();
        float fillFrac = maxCharges > 0 ? (float) remaining / maxCharges : 0;
        int fillW = (int) (BAR_W * fillFrac);
        if (fillW > 0) {
            g.fill(barX, row2Y, barX + fillW, row2Y + BAR_H, scentColor);
        }

        int row3Y = y + 48;
        int cooldownTicks = this.menu.getCooldownTicks();
        int maxCooldownTicks = this.menu.getMaxCooldownTicks();

        drawClockIcon(g, infoX, row3Y);

        if (cooldownTicks <= 0) {
            g.drawString(
                    this.font,
                    Texts.tr("gui.aromaaffect.omara_device.ready"),
                    infoX + 8,
                    row3Y,
                    Colors.SUCCESS_GREEN_LIGHT,
                    true);
        } else {
            float secs = cooldownTicks / 20.0f;
            String text;
            if (secs >= 60) {
                text = (int) (secs / 60) + "m" + (int) (secs % 60) + "s";
            } else {
                text = String.format("%.0fs", secs);
            }
            g.drawString(this.font, text, infoX + 8, row3Y, Colors.WARNING_ORANGE, true);

            int pBarX = infoX + 8;
            int pBarY = row3Y + 11;
            g.fill(pBarX, pBarY, pBarX + BAR_W, pBarY + 2, Colors.BG_DARK_PANEL);
            if (maxCooldownTicks > 0) {
                float progress = 1.0f - (float) cooldownTicks / maxCooldownTicks;
                int pFill = (int) (BAR_W * progress);
                if (pFill > 0) {
                    g.fill(pBarX, pBarY, pBarX + pFill, pBarY + 2, scentColor);
                }
            }
        }
    }

    private void renderModeButton(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int bx = x + BTN_X;
        int by = y + BTN_MODE_Y;

        boolean hovered = isInBounds(mouseX, mouseY, bx, by, BTN_W, BTN_H);
        drawButton(g, bx, by, BTN_W, BTN_H, hovered);

        boolean isAuto = this.menu.getMode() == 0;
        Component label =
                Texts.tr(
                        isAuto
                                ? "gui.aromaaffect.omara_device.mode.auto"
                                : "gui.aromaaffect.omara_device.mode.redstone");
        int textColor = isAuto ? Colors.SUCCESS_GREEN_LIGHT : Colors.ERROR_RED_LIGHT;

        int textW = this.font.width(label);
        g.drawString(this.font, label, bx + (BTN_W - textW) / 2, by + 2, textColor, true);
    }

    private void renderIntervalButton(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int bx = x + BTN_X;
        int by = y + BTN_INTERVAL_Y;

        boolean hovered = isInBounds(mouseX, mouseY, bx, by, BTN_W, BTN_H);
        drawButton(g, bx, by, BTN_W, BTN_H, hovered);

        boolean is60s = this.menu.getIntervalIndex() == 0;
        Component label =
                Texts.tr(
                        is60s
                                ? "gui.aromaaffect.omara_device.interval.60s"
                                : "gui.aromaaffect.omara_device.interval.5min");

        int textW = this.font.width(label);
        g.drawString(this.font, label, bx + (BTN_W - textW) / 2, by + 2, Colors.WHITE, true);
    }

    private void drawButton(GuiGraphics g, int bx, int by, int w, int h, boolean hovered) {
        int bg = hovered ? Colors.BG_ROW : Colors.BG_ROW_ALT;
        g.fill(bx, by, bx + w, by + h, bg);
        g.fill(bx, by, bx + w, by + 1, Colors.TEXT_DISABLED);
        g.fill(bx, by + h - 1, bx + w, by + h, Colors.BG_ROW_SUBTLE);
        g.fill(bx, by, bx + 1, by + h, Colors.TEXT_DISABLED);
        g.fill(bx + w - 1, by, bx + w, by + h, Colors.BG_ROW_SUBTLE);
    }

    private void drawBatteryIcon(GuiGraphics g, int x, int y, int color) {
        int b = Colors.TEXT_MUTED;
        g.fill(x, y, x + 5, y + 1, b);
        g.fill(x, y + 4, x + 5, y + 5, b);
        g.fill(x, y, x + 1, y + 5, b);
        g.fill(x + 4, y, x + 5, y + 5, b);
        g.fill(x + 5, y + 1, x + 6, y + 4, b);
        g.fill(x + 1, y + 1, x + 4, y + 4, color);
    }

    private void drawClockIcon(GuiGraphics g, int x, int y) {
        int c = Colors.TEXT_MUTED;
        g.fill(x + 1, y, x + 4, y + 1, c);
        g.fill(x + 1, y + 4, x + 4, y + 5, c);
        g.fill(x, y + 1, x + 1, y + 4, c);
        g.fill(x + 4, y + 1, x + 5, y + 4, c);
        g.fill(x + 2, y + 2, x + 3, y + 3, Colors.WHITE);
        g.fill(x + 2, y + 1, x + 3, y + 2, Colors.WHITE);
        g.fill(x + 3, y + 2, x + 4, y + 3, Colors.WHITE);
    }

    private String trimToWidth(String text, int maxPixels) {
        if (this.font.width(text) <= maxPixels) return text;
        String ellipsis = "..";
        int ellipsisW = this.font.width(ellipsis);
        for (int i = text.length() - 1; i > 0; i--) {
            if (this.font.width(text.substring(0, i)) + ellipsisW <= maxPixels) {
                return text.substring(0, i) + ellipsis;
            }
        }
        return ellipsis;
    }

    private static boolean isInBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
