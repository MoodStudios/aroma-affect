package com.ovrtechnology.entity.sniffer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.ovrtechnology.menu.ResponsiveContainerScreen;
import com.ovrtechnology.menu.MenuRenderUtils;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SnifferScreen extends ResponsiveContainerScreen<SnifferMenu> {

    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier HORSE_INVENTORY_LOCATION =
            Identifier.withDefaultNamespace("textures/gui/container/horse.png");

    private float xMouse;
    private float yMouse;

    public SnifferScreen(SnifferMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void extractContainerBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;

        // Fondo principal (igual que HorseInventoryScreen)
        graphics.blit(RenderPipelines.GUI_TEXTURED, HORSE_INVENTORY_LOCATION, k, l, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // Slot de silla (saddle) - posición k+7, l+17
        drawSlot(graphics, k + 7, l + 35 - 18);

        // Slot de decoración (armor) - posición k+7, l+35
        drawSlot(graphics, k + 7, l + 35);

        // Renderizar el Sniffer en la pantalla
        if (this.menu.getSniffer() != null) {
            MenuRenderUtils.renderEntityInViewport(
                    graphics,
                    k + 26, l + 18,
                    k + 78, l + 70,
                    17,
                    0.25F,
                    this.xMouse, this.yMouse,
                    this.menu.getSniffer()
            );
        }
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
    }

    @Override
    protected void extractContainerContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.xMouse = (float) mouseX;
        this.yMouse = (float) mouseY;
        super.extractContainerContent(graphics, mouseX, mouseY, partialTick);
        this.extractTooltip(graphics, mouseX, mouseY);
    }
}
