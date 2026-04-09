package com.ovrtechnology.tutorial.demo;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

/**
 * F6 = New Game (quick restart) during Play mode.
 * Only active when a demo session is running (not in edit mode).
 */
public final class DemoKeyHandler {

    private static boolean initialized = false;
    private static boolean wasPressed = false;

    private DemoKeyHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.player == null) return;
            if (!DemoWorldManager.hasTemplate()) return;
            if (!DemoWorldManager.isSessionActive()) return;
            if (DemoWorldManager.isEditSession()) return;

            boolean pressed = InputConstants.isKeyDown(
                    Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_F6);

            if (pressed && !wasPressed) {
                // Set flags — MinecraftMixin will process disconnect at top of next runTick
                DemoWorldManager.scheduleNewGame();
                DemoWorldManager.scheduleDisconnect();
            }

            wasPressed = pressed;
        });
    }
}
