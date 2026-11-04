package com.ovrtechnology.legacy.balm.client.callback;

import com.ovrtechnology.legacy.balm.LegacyEvent;
import com.ovrtechnology.legacy.gui.GuiGraphicsExtractor;
import com.ovrtechnology.legacy.input.MouseButtonEvent;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.client.screen.ScreenDrawEvent;
import net.blay09.mods.balm.api.event.client.screen.ScreenInitEvent;
import net.blay09.mods.balm.api.event.client.screen.ScreenMouseEvent;
import net.minecraft.client.gui.screens.Screen;

public interface ScreenCallback {

    @FunctionalInterface
    interface Render {
        void render(Screen screen, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float tickDelta);

        LegacyEvent<Render> BEFORE = new LegacyEvent<>(h -> Balm.getEvents().onEvent(ScreenDrawEvent.Pre.class, e -> {
            GuiGraphicsExtractor g = new GuiGraphicsExtractor(e.getGuiGraphics());
            h.render(e.getScreen(), g, e.getMouseX(), e.getMouseY(), e.getTickDelta());
            g.flush();
        }));

        LegacyEvent<Render> AFTER = new LegacyEvent<>(h -> Balm.getEvents().onEvent(ScreenDrawEvent.Post.class, e -> {
            GuiGraphicsExtractor g = new GuiGraphicsExtractor(e.getGuiGraphics());
            h.render(e.getScreen(), g, e.getMouseX(), e.getMouseY(), e.getTickDelta());
            g.flush();
        }));
    }

    interface Init {
        @FunctionalInterface
        interface Before {
            void beforeInit(Screen screen);

            LegacyEvent<Before> EVENT = new LegacyEvent<>(h -> Balm.getEvents().onEvent(ScreenInitEvent.Pre.class, e -> h.beforeInit(e.getScreen())));
        }

        @FunctionalInterface
        interface After {
            void afterInit(Screen screen);

            LegacyEvent<After> EVENT = new LegacyEvent<>(h -> Balm.getEvents().onEvent(ScreenInitEvent.Post.class, e -> h.afterInit(e.getScreen())));
        }
    }

    interface MousePress {
        @FunctionalInterface
        interface Before {
            boolean mousePressed(Screen screen, MouseButtonEvent event);

            LegacyEvent<Before> EVENT = new LegacyEvent<>(h -> Balm.getEvents().onEvent(ScreenMouseEvent.Click.Pre.class, e -> {
                MouseButtonEvent event = new MouseButtonEvent(e.getMouseX(), e.getMouseY(), e.getButton());
                if (h.mousePressed(e.getScreen(), event)) {
                    e.setCanceled(true);
                }
            }));
        }
    }
}
