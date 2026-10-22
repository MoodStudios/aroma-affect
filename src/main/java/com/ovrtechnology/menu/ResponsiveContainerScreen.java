package com.ovrtechnology.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Keeps inventory slots, their artwork and mouse coordinates in the same viewport. */
public abstract class ResponsiveContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected ResponsiveContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override public final void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        extractContainerBackground(graphics, x, y, tick);
    }

    protected abstract void extractContainerBackground(GuiGraphicsExtractor graphics, int x, int y, float tick);

    @Override public final void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        extractContainerContent(graphics, x, y, tick);
    }

    protected void extractContainerContent(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        super.extractRenderState(graphics, x, y, tick);
    }

    @Override public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return handleContainerClick(event, doubleClick);
    }

    protected boolean handleContainerClick(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

}
