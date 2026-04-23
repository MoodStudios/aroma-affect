package com.ovrtechnology.nose.client;

import com.ovrtechnology.mixin.AbstractContainerScreenAccessor;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.hooks.client.screen.ScreenAccess;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

public final class NoseInventoryUi {
    private static final int HEAD_CONTAINER_SLOT = 39;
    private static final int BUTTON_X_OFFSET = 18 + 2;
    private static final int BUTTON_Y_OFFSET = 2;
    private static boolean initialized = false;
    private static final Map<Screen, NoseStrapToggleButton> strapButtons = new WeakHashMap<>();

    private NoseInventoryUi() {}

    public static void init() {
        if (initialized) {
            return;
        }

        ClientGuiEvent.INIT_POST.register(NoseInventoryUi::onInitPost);
        ClientGuiEvent.RENDER_CONTAINER_FOREGROUND.register(
                NoseInventoryUi::onRenderContainerForeground);
        initialized = true;
    }

    private static void onInitPost(Screen screen, ScreenAccess access) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }
        if (!(containerScreen.getMenu() instanceof InventoryMenu)) {
            return;
        }

        NoseStrapToggleButton button =
                new NoseStrapToggleButton(0, 0, b -> NoseRenderToggles.toggleStrapEnabled());
        access.addRenderableWidget(button);
        strapButtons.put(screen, button);
        positionButton(containerScreen, button);
    }

    private static void onRenderContainerForeground(
            AbstractContainerScreen<?> screen,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float delta) {
        NoseStrapToggleButton button = strapButtons.get(screen);
        if (button == null) {
            return;
        }

        positionButton(screen, button);
    }

    private static void positionButton(
            AbstractContainerScreen<?> screen, NoseStrapToggleButton button) {
        Optional<Slot> headSlotOpt = findHeadSlot(screen.getMenu());
        if (!(screen instanceof AbstractContainerScreenAccessor accessor)
                || headSlotOpt.isEmpty()) {
            button.visible = false;
            return;
        }

        Slot headSlot = headSlotOpt.get();
        button.visible = true;
        button.setX(accessor.aromaaffect$getLeftPos() + headSlot.x + BUTTON_X_OFFSET);
        button.setY(accessor.aromaaffect$getTopPos() + headSlot.y + BUTTON_Y_OFFSET);
    }

    private static Optional<Slot> findHeadSlot(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory
                    && slot.getContainerSlot() == HEAD_CONTAINER_SLOT) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }
}
