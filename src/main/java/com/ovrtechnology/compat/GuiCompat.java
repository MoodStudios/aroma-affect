package com.ovrtechnology.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

public final class GuiCompat {

    private GuiCompat() {}

    public static void renderWidget(AbstractWidget widget, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        //? if >=26 {
        widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        //?} else {
        /*widget.render(graphics.raw(), mouseX, mouseY, partialTick);
        *///?}
    }
}
