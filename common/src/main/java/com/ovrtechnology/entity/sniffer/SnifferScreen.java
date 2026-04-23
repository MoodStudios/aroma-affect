package com.ovrtechnology.entity.sniffer;

import com.ovrtechnology.util.Ids;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SnifferScreen extends AbstractContainerScreen<SnifferMenu> {

    private static final ResourceLocation SLOT_SPRITE = Ids.vanilla("container/slot");
    private static final ResourceLocation HORSE_INVENTORY_LOCATION =
            Ids.vanilla("textures/gui/container/horse.png");

    private float xMouse;
    private float yMouse;

    public SnifferScreen(SnifferMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(
                HORSE_INVENTORY_LOCATION,
                k,
                l,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                256,
                256);

        drawSlot(guiGraphics, k + 7, l + 35 - 18);

        drawSlot(guiGraphics, k + 7, l + 35);

        if (this.menu.getSniffer() != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    k + 26,
                    l + 18,
                    k + 78,
                    l + 70,
                    17,
                    0.25F,
                    this.xMouse,
                    this.yMouse,
                    this.menu.getSniffer());
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blitSprite(SLOT_SPRITE, x, y, 18, 18);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.xMouse = (float) mouseX;
        this.yMouse = (float) mouseY;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
