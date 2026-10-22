package com.ovrtechnology.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Replacement for Architectury's {@code ScreenAccess#addRenderableWidget}
 * helper. Used by {@link com.ovrtechnology.nose.client.NoseInventoryUi} to
 * inject the strap-toggle button into the inventory screen during
 * {@code ScreenCallback.Init.After}.
 */
@Mixin(Screen.class)
public interface ScreenInvoker {

    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T aromaaffect$addRenderableWidget(T widget);
}
