package com.ovrtechnology.entity.sniffer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SnifferScreen extends AbstractContainerScreen<SnifferMenu> {

    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier HORSE_INVENTORY_LOCATION =
            Identifier.withDefaultNamespace("textures/gui/container/horse.png");

    private float xMouse;
    private float yMouse;

    public SnifferScreen(SnifferMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;

        // Fondo principal (igual que HorseInventoryScreen)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, HORSE_INVENTORY_LOCATION, k, l, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // Slot de silla (saddle) - posición k+7, l+17
        drawSlot(guiGraphics, k + 7, l + 35 - 18);

        // Slot de decoración (armor) - posición k+7, l+35
        drawSlot(guiGraphics, k + 7, l + 35);

        // Renderizar el Sniffer en la pantalla
        if (this.menu.getSniffer() != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    k + 26, l + 18,
                    k + 78, l + 70,
                    17,
                    0.25F,
                    this.xMouse, this.yMouse,
                    this.menu.getSniffer()
            );
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.xMouse = (float) mouseX;
        this.yMouse = (float) mouseY;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
