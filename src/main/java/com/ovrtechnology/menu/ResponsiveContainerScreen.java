package com.ovrtechnology.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
//? if <26 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/** Keeps inventory slots, their artwork and mouse coordinates in the same viewport. */
public abstract class ResponsiveContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected ResponsiveContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    //? if >=26 {
    @Override public final void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        extractContainerBackground(graphics, x, y, tick);
    }
    //?} else {
    /*@Override
    protected final void renderBg(GuiGraphics guiGraphics, float tick, int x, int y) {
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(guiGraphics);
        extractContainerBackground(graphics, x, y, tick);
        graphics.flush();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int x, int y, float tick) {
        renderBg(guiGraphics, tick, x, y);
    }
    *///?}

    protected abstract void extractContainerBackground(GuiGraphicsExtractor graphics, int x, int y, float tick);

    //? if >=26 {
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
    //?} else {
    /*@Override
    public final void render(GuiGraphics guiGraphics, int x, int y, float tick) {
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(guiGraphics);
        extractContainerContent(graphics, x, y, tick);
        graphics.flush();
    }

    protected void extractContainerContent(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        super.render(graphics.raw(), x, y, tick);
    }

    protected void extractTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.defer(() -> this.renderTooltip(graphics.raw(), x, y));
    }

    @Override
    public final boolean mouseClicked(double x, double y, int button) {
        return handleContainerClick(new MouseButtonEvent(x, y, button), false);
    }

    protected boolean handleContainerClick(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event.x(), event.y(), event.button());
    }
    *///?}

}
