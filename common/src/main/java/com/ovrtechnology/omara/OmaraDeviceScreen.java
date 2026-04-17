package com.ovrtechnology.omara;

import com.ovrtechnology.util.Texts;
import com.ovrtechnology.util.Ids;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.menu.MenuRenderUtils;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.scentitem.ScentItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class OmaraDeviceScreen extends AbstractContainerScreen<OmaraDeviceMenu> {

    private static final ResourceLocation TEXTURE = Ids.mod("textures/gui/container/omara_device.png");

    // Info panel starts right of the slot area
    private static final int INFO_X = 98;

    // Bar dimensions (compact to avoid overflow)
    private static final int BAR_W = 40;
    private static final int BAR_H = 5;

    // Buttons: stacked vertically, interval above mode, above "Inventory" label
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
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
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
        // Scent triggering is now handled server-side via OmaraDeviceNetworking.
        // The server broadcasts to all nearby players when the device puffs.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isValidClickButton) {
        if (event.button() == 0) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            double mx = event.x();
            double my = event.y();

            // Mode button
            if (isInBounds(mx, my, x + BTN_X, y + BTN_MODE_Y, BTN_W, BTN_H)) {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    MenuRenderUtils.playClickSound();
                }
                return true;
            }

            // Interval button (only in auto mode)
            if (this.menu.getMode() == 0 && isInBounds(mx, my, x + BTN_X, y + BTN_INTERVAL_Y, BTN_W, BTN_H)) {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    MenuRenderUtils.playClickSound();
                }
                return true;
            }
        }

        return super.mouseClicked(event, isValidClickButton);
    }

    // ========================================
    // Info Panel (right of slot)
    // ========================================

    private void renderInfoPanel(GuiGraphics g, int x, int y) {
        ItemStack capsule = this.menu.getSlot(0).getItem();
        int infoX = x + INFO_X;

        if (capsule.isEmpty() || !(capsule.getItem() instanceof ScentItem si) || !si.getDefinition().isCapsule()) {
            return;
        }

        String scentName = si.getDefinition().getScent();
        String fallbackName = si.getDefinition().getFallbackName();

        // Scent color
        int[] rgb = {255, 255, 255};
        Optional<ScentDefinition> scentOpt = ScentRegistry.getScentByName(scentName);
        if (scentOpt.isPresent()) {
            rgb = scentOpt.get().getColorRGB();
        }
        int scentColor = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];

        // ── Row 1: Scent name with color dot ──
        int row1Y = y + 24;
        g.fill(infoX, row1Y + 1, infoX + 4, row1Y + 5, scentColor);

        // Truncate name to fit within GUI bounds
        int maxNamePx = (x + this.imageWidth - 4) - (infoX + 6);
        String displayName = trimToWidth(fallbackName, maxNamePx);
        g.drawString(this.font, displayName, infoX + 6, row1Y, 0xFFFFFFFF, true);

        // ── Row 2: Charges bar (no text) ──
        int row2Y = y + 36;
        drawBatteryIcon(g, infoX, row2Y, scentColor);

        int barX = infoX + 8;
        g.fill(barX, row2Y, barX + BAR_W, row2Y + BAR_H, 0xFF333333);

        int remaining = capsule.getMaxDamage() - capsule.getDamageValue();
        int maxCharges = capsule.getMaxDamage();
        float fillFrac = maxCharges > 0 ? (float) remaining / maxCharges : 0;
        int fillW = (int) (BAR_W * fillFrac);
        if (fillW > 0) {
            g.fill(barX, row2Y, barX + fillW, row2Y + BAR_H, scentColor);
        }

        // ── Row 3: Cooldown timer ──
        int row3Y = y + 48;
        int cooldownTicks = this.menu.getCooldownTicks();
        int maxCooldownTicks = this.menu.getMaxCooldownTicks();

        drawClockIcon(g, infoX, row3Y);

        if (cooldownTicks <= 0) {
            g.drawString(this.font,
                    Texts.tr("gui.aromaaffect.omara_device.ready"),
                    infoX + 8, row3Y, 0xFF55FF55, true);
        } else {
            float secs = cooldownTicks / 20.0f;
            String text;
            if (secs >= 60) {
                text = (int) (secs / 60) + "m" + (int) (secs % 60) + "s";
            } else {
                text = String.format("%.0fs", secs);
            }
            g.drawString(this.font, text, infoX + 8, row3Y, 0xFFFFAA00, true);

            // Progress bar
            int pBarX = infoX + 8;
            int pBarY = row3Y + 11;
            g.fill(pBarX, pBarY, pBarX + BAR_W, pBarY + 2, 0xFF333333);
            if (maxCooldownTicks > 0) {
                float progress = 1.0f - (float) cooldownTicks / maxCooldownTicks;
                int pFill = (int) (BAR_W * progress);
                if (pFill > 0) {
                    g.fill(pBarX, pBarY, pBarX + pFill, pBarY + 2, scentColor);
                }
            }
        }
    }

    // ========================================
    // Buttons (bottom of grid, above "Inventory")
    // ========================================

    private void renderModeButton(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int bx = x + BTN_X;
        int by = y + BTN_MODE_Y;

        boolean hovered = isInBounds(mouseX, mouseY, bx, by, BTN_W, BTN_H);
        drawButton(g, bx, by, BTN_W, BTN_H, hovered);

        boolean isAuto = this.menu.getMode() == 0;
        Component label = Texts.tr(isAuto
                ? "gui.aromaaffect.omara_device.mode.auto"
                : "gui.aromaaffect.omara_device.mode.redstone");
        int textColor = isAuto ? 0xFF55FF55 : 0xFFFF5555;

        // Center text in button
        int textW = this.font.width(label);
        g.drawString(this.font, label, bx + (BTN_W - textW) / 2, by + 2, textColor, true);
    }

    private void renderIntervalButton(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int bx = x + BTN_X;
        int by = y + BTN_INTERVAL_Y;

        boolean hovered = isInBounds(mouseX, mouseY, bx, by, BTN_W, BTN_H);
        drawButton(g, bx, by, BTN_W, BTN_H, hovered);

        boolean is60s = this.menu.getIntervalIndex() == 0;
        Component label = Texts.tr(is60s
                ? "gui.aromaaffect.omara_device.interval.60s"
                : "gui.aromaaffect.omara_device.interval.5min");

        int textW = this.font.width(label);
        g.drawString(this.font, label, bx + (BTN_W - textW) / 2, by + 2, 0xFFFFFFFF, true);
    }

    private void drawButton(GuiGraphics g, int bx, int by, int w, int h, boolean hovered) {
        int bg = hovered ? 0xFF555555 : 0xFF3A3A3A;
        g.fill(bx, by, bx + w, by + h, bg);
        g.fill(bx, by, bx + w, by + 1, 0xFF666666);           // top highlight
        g.fill(bx, by + h - 1, bx + w, by + h, 0xFF222222);   // bottom shadow
        g.fill(bx, by, bx + 1, by + h, 0xFF666666);           // left highlight
        g.fill(bx + w - 1, by, bx + w, by + h, 0xFF222222);   // right shadow
    }

    // ========================================
    // Pixel-art icons
    // ========================================

    private void drawBatteryIcon(GuiGraphics g, int x, int y, int color) {
        int b = 0xFFAAAAAA;
        g.fill(x, y, x + 5, y + 1, b);
        g.fill(x, y + 4, x + 5, y + 5, b);
        g.fill(x, y, x + 1, y + 5, b);
        g.fill(x + 4, y, x + 5, y + 5, b);
        g.fill(x + 5, y + 1, x + 6, y + 4, b);
        g.fill(x + 1, y + 1, x + 4, y + 4, color);
    }

    private void drawClockIcon(GuiGraphics g, int x, int y) {
        int c = 0xFFAAAAAA;
        g.fill(x + 1, y, x + 4, y + 1, c);
        g.fill(x + 1, y + 4, x + 4, y + 5, c);
        g.fill(x, y + 1, x + 1, y + 4, c);
        g.fill(x + 4, y + 1, x + 5, y + 4, c);
        g.fill(x + 2, y + 2, x + 3, y + 3, 0xFFFFFFFF);
        g.fill(x + 2, y + 1, x + 3, y + 2, 0xFFFFFFFF);
        g.fill(x + 3, y + 2, x + 4, y + 3, 0xFFFFFFFF);
    }

    // ========================================
    // Utility
    // ========================================

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
