package com.ovrtechnology.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Keeps inventory slots, their artwork and mouse coordinates in the same viewport. */
public abstract class ResponsiveContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private MenuViewport viewport = MenuViewport.fit(1, 1, 1, 1);

    protected ResponsiveContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected void init() {
        viewport = MenuViewport.fitPixels(minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight(), minecraft.getWindow().getGuiScale(), imageWidth + 16, imageHeight + 16);
        width = viewport.width();
        height = viewport.height();
        super.init();
    }

    private void push(GuiGraphicsExtractor graphics) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) viewport.offsetX(), (float) viewport.offsetY());
        graphics.pose().scale((float) viewport.scale(), (float) viewport.scale());
    }

    @Override public final void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        push(graphics);
        try { extractContainerBackground(graphics, (int) viewport.localX(x), (int) viewport.localY(y), tick); }
        finally { graphics.pose().popMatrix(); }
    }

    protected abstract void extractContainerBackground(GuiGraphicsExtractor graphics, int x, int y, float tick);

    @Override public final void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        push(graphics);
        try { extractContainerContent(graphics, (int) viewport.localX(x), (int) viewport.localY(y), tick); }
        finally { graphics.pose().popMatrix(); }
    }

    protected void extractContainerContent(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        super.extractRenderState(graphics, x, y, tick);
    }

    @Override protected void extractTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        // Vanilla draws deferred tooltips after our layout transform has been removed.
        super.extractTooltip(graphics, viewport.screenX(x), viewport.screenY(y));
    }

    private MouseButtonEvent local(MouseButtonEvent event) {
        return new MouseButtonEvent(viewport.localX(event.x()), viewport.localY(event.y()), event.buttonInfo());
    }

    @Override public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return handleContainerClick(local(event), doubleClick);
    }

    protected boolean handleContainerClick(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) { return super.mouseReleased(local(event)); }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(local(event), dx / viewport.scale(), dy / viewport.scale());
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        return super.mouseScrolled(viewport.localX(x), viewport.localY(y), dx, dy);
    }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(viewport.localX(x), viewport.localY(y)); }
}
