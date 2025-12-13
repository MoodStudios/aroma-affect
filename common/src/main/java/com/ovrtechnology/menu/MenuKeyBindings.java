package com.ovrtechnology.menu;

import com.ovrtechnology.AromaCraft;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Handles keybinding registration and input handling for AromaCraft menus.
 * 
 * <p>This class uses Architectury's cross-platform keybinding API to register
 * and handle menu hotkeys on both Fabric and NeoForge.</p>
 * 
 * <h3>Default Keybindings:</h3>
 * <ul>
 *   <li><b>Radial Menu</b>: R - Opens the main radial menu for selecting tracking categories</li>
 *   <li><b>Config Menu</b>: C (with Shift) - Opens the configuration screen</li>
 * </ul>
 * 
 * <p>All keybindings are configurable through Minecraft's controls menu under the "AromaCraft" category.</p>
 */
public final class MenuKeyBindings {
    
    /**
     * The AromaCraft keybinding category.
     */
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            ResourceLocation.fromNamespaceAndPath(AromaCraft.MOD_ID, "keybinds")
    );
    
    /**
     * Keybinding for opening the radial menu.
     */
    public static final KeyMapping OPEN_RADIAL_MENU = new KeyMapping(
            "key.aromacraft.open_radial_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );
    
    /**
     * Keybinding for opening the configuration menu.
     */
    public static final KeyMapping OPEN_CONFIG_MENU = new KeyMapping(
            "key.aromacraft.open_config_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );
    
    /**
     * Whether the key bindings have been initialized.
     */
    private static boolean initialized = false;
    
    private MenuKeyBindings() {
        // Utility class
    }
    
    /**
     * Initializes the menu keybindings.
     * Should be called during client-side mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaCraft.LOGGER.warn("MenuKeyBindings.init() called multiple times!");
            return;
        }
        
        AromaCraft.LOGGER.info("Initializing menu keybindings...");
        
        // Register keybindings using Architectury API
        KeyMappingRegistry.register(OPEN_RADIAL_MENU);
        KeyMappingRegistry.register(OPEN_CONFIG_MENU);
        
        // Register tick handler to check for key presses
        ClientTickEvent.CLIENT_POST.register(instance -> {
            handleKeyInputs();
        });
        
        initialized = true;
        AromaCraft.LOGGER.info("Menu keybindings initialized");
    }
    
    /**
     * Handles key input checks during client tick.
     * Called every client tick to check for keybinding presses.
     */
    private static void handleKeyInputs() {
        Minecraft minecraft = Minecraft.getInstance();
        
        // Don't process keybindings if no player or a screen is already open
        if (minecraft.player == null) {
            return;
        }
        
        // Check for radial menu key
        while (OPEN_RADIAL_MENU.consumeClick()) {
            if (minecraft.screen == null) {
                // Open radial menu when no screen is open
                MenuManager.openRadialMenu();
            } else if (minecraft.screen instanceof RadialMenuScreen) {
                // Close radial menu if it's already open (toggle behavior)
                minecraft.setScreen(null);
            }
        }
        
        // Check for config menu key (only when holding Shift)
        while (OPEN_CONFIG_MENU.consumeClick()) {
            if (minecraft.screen == null && isShiftDown()) {
                MenuManager.openConfigMenu();
            }
        }
    }
    
    /**
     * Checks if the Shift key is currently held.
     */
    private static boolean isShiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
               InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
    
    /**
     * Checks if the radial menu keybinding is pressed.
     * Useful for external checks without consuming the click.
     */
    public static boolean isRadialMenuKeyDown() {
        return OPEN_RADIAL_MENU.isDown();
    }
    
    /**
     * Gets the display name of the radial menu keybinding.
     * Useful for displaying the key in tooltips or instructions.
     */
    public static String getRadialMenuKeyName() {
        return OPEN_RADIAL_MENU.getTranslatedKeyMessage().getString();
    }
    
    /**
     * Gets the display name of the config menu keybinding.
     */
    public static String getConfigMenuKeyName() {
        return OPEN_CONFIG_MENU.getTranslatedKeyMessage().getString();
    }
}
