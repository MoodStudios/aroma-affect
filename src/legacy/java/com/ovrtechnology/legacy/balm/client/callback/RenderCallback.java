package com.ovrtechnology.legacy.balm.client.callback;

import com.mojang.blaze3d.platform.Window;
import com.ovrtechnology.legacy.balm.LegacyEvent;
import com.ovrtechnology.legacy.gui.GuiGraphicsExtractor;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.client.GuiDrawEvent;

public interface RenderCallback {

    interface Gui {

        @FunctionalInterface
        interface Before {
            boolean shouldRender(GuiGraphicsExtractor guiGraphics, Window window);
        }

        @FunctionalInterface
        interface After {
            void afterRender(GuiGraphicsExtractor guiGraphics, Window window);
        }

        LegacyEvent<Before> BEFORE = new LegacyEvent<>(h -> Balm.getEvents().onEvent(GuiDrawEvent.Pre.class, e -> {
            if (e.getElement() == GuiDrawEvent.Element.ALL) {
                GuiGraphicsExtractor g = new GuiGraphicsExtractor(e.getGuiGraphics());
                if (!h.shouldRender(g, e.getWindow())) {
                    e.setCanceled(true);
                }
                g.flush();
            }
        }));

        LegacyEvent<After> AFTER = new LegacyEvent<>(h -> Balm.getEvents().onEvent(GuiDrawEvent.Post.class, e -> {
            if (e.getElement() == GuiDrawEvent.Element.ALL) {
                GuiGraphicsExtractor g = new GuiGraphicsExtractor(e.getGuiGraphics());
                h.afterRender(g, e.getWindow());
                g.flush();
            }
        }));
    }
}
