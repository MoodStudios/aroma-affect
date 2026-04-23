package com.ovrtechnology.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.keybind.AromaAffectKeyCategory;
import com.ovrtechnology.trigger.ScentTriggerManager;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class MenuKeyBindings {

    public static final KeyMapping OPEN_RADIAL_MENU =
            new KeyMapping(
                    "key.aromaaffect.open_radial_menu",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    AromaAffectKeyCategory.CATEGORY);

    public static final KeyMapping OPEN_CONFIG_MENU =
            new KeyMapping(
                    "key.aromaaffect.open_config_menu",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_C,
                    AromaAffectKeyCategory.CATEGORY);

    public static final KeyMapping RESET_COOLDOWNS =
            new KeyMapping(
                    "key.aromaaffect.reset_cooldowns",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_X,
                    AromaAffectKeyCategory.CATEGORY);

    private static boolean initialized = false;

    private MenuKeyBindings() {}

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("MenuKeyBindings.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing menu keybindings...");

        KeyMappingRegistry.register(OPEN_RADIAL_MENU);
        KeyMappingRegistry.register(OPEN_CONFIG_MENU);
        KeyMappingRegistry.register(RESET_COOLDOWNS);

        ClientTickEvent.CLIENT_POST.register(
                instance -> {
                    handleKeyInputs();
                });

        initialized = true;
        AromaAffect.LOGGER.info("Menu keybindings initialized");
    }

    private static void handleKeyInputs() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        while (OPEN_RADIAL_MENU.consumeClick()) {
            if (minecraft.screen == null) {

                MenuManager.openRadialMenu();
            } else if (minecraft.screen instanceof RadialMenuScreen) {

                minecraft.setScreen(null);
            }
        }

        while (OPEN_CONFIG_MENU.consumeClick()) {
            if (minecraft.screen == null && isShiftDown()) {
                MenuManager.openConfigMenu();
            }
        }

        while (RESET_COOLDOWNS.consumeClick()) {
            if (minecraft.screen == null) {
                ScentTriggerManager.getInstance().resetCooldowns();
            }
        }
    }

    private static boolean isShiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
