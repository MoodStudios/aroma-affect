package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.keybind.AromaAffectKeyCategory;
import com.ovrtechnology.trigger.ScentTriggerManager;
import net.blay09.mods.balm.client.BalmKeyMappingRegistrar;
import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Handles keybinding registration and input handling for Aroma Affect menus.
 *
 * <p>Keymapping registration goes through Balm's {@link BalmKeyMappingRegistrar}
 * (called by AromaAffectClient.initialize via the client registrars). The tick
 * handler that polls the keybinds is registered separately via
 * {@link #initTickHandler()}.</p>
 */
public final class MenuKeyBindings {

    public static final KeyMapping OPEN_RADIAL_MENU = new KeyMapping(
            "key.aromaaffect.open_radial_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            AromaAffectKeyCategory.CATEGORY
    );

    public static final KeyMapping OPEN_CONFIG_MENU = new KeyMapping(
            "key.aromaaffect.open_config_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            AromaAffectKeyCategory.CATEGORY
    );

    public static final KeyMapping RESET_COOLDOWNS = new KeyMapping(
            "key.aromaaffect.reset_cooldowns",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            AromaAffectKeyCategory.CATEGORY
    );

    private static boolean tickHandlerInitialized = false;

    private MenuKeyBindings() {}

    /**
     * Balm key-mapping registrar callback. Wire from
     * {@code AromaAffectClient.initialize} via
     * {@code registrars.keyMappings(MenuKeyBindings::register)}.
     */
    public static void register(BalmKeyMappingRegistrar keyMappings) {
        keyMappings.register(OPEN_RADIAL_MENU);
        keyMappings.register(OPEN_CONFIG_MENU);
        keyMappings.register(RESET_COOLDOWNS);
        AromaAffect.LOGGER.info("Menu keybindings registered");
    }

    /**
     * Registers the client tick handler that polls the keybinds. Idempotent.
     */
    public static void initTickHandler() {
        if (tickHandlerInitialized) {
            return;
        }
        tickHandlerInitialized = true;
        ClientTickCallback.AFTER.register(instance -> handleKeyInputs());
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
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
               InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isRadialMenuKeyDown() {
        return OPEN_RADIAL_MENU.isDown();
    }

    public static String getRadialMenuKeyName() {
        return OPEN_RADIAL_MENU.getTranslatedKeyMessage().getString();
    }

    public static String getConfigMenuKeyName() {
        return OPEN_CONFIG_MENU.getTranslatedKeyMessage().getString();
    }
}
