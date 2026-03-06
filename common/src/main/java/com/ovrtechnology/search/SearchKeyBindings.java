package com.ovrtechnology.search;

import com.ovrtechnology.AromaAffect;

import com.ovrtechnology.keybind.AromaAffectKeyCategory;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Handles keybinding registration and input handling for the Aroma Affect search system.
 * 
 * <p>This class uses Architectury's cross-platform keybinding API to register
 * and handle search-related hotkeys on both Fabric and NeoForge.</p>
 * 
 * <h3>Default Keybindings:</h3>
 * <ul>
 *   <li><b>Toggle Search</b>: V - Activates/deactivates search mode when a Nose is equipped</li>
 * </ul>
 * 
 * <p>All keybindings are configurable through Minecraft's controls menu under the "Aroma Affect" category.</p>
 */
public final class SearchKeyBindings {
    
    /**
     * Keybinding for toggling search mode on/off.
     * Default key: V
     */
    public static final KeyMapping TOGGLE_SEARCH = new KeyMapping(
            "key.aromaaffect.toggle_search",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            AromaAffectKeyCategory.CATEGORY
    );
    
    /**
     * Whether the key bindings have been initialized.
     */
    private static boolean initialized = false;
    
    private SearchKeyBindings() {
        // Utility class
    }
    
    /**
     * Initializes the search keybindings.
     * Should be called during client-side mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("SearchKeyBindings.init() called multiple times!");
            return;
        }
        
        AromaAffect.LOGGER.info("Initializing search keybindings...");
        
        // Initialize the search manager first
        SearchManager.init();
        
        // Register keybindings using Architectury API
        KeyMappingRegistry.register(TOGGLE_SEARCH);
        
        // Register tick handler to check for key presses
        ClientTickEvent.CLIENT_POST.register(instance -> handleKeyInputs());
        
        initialized = true;
        AromaAffect.LOGGER.info("Search keybindings initialized (default: V key)");
    }
    
    /**
     * Handles key input checks during client tick.
     * Called every client tick to check for keybinding presses.
     */
    private static void handleKeyInputs() {
        Minecraft minecraft = Minecraft.getInstance();
        
        // Don't process keybindings if no player
        if (minecraft.player == null) {
            return;
        }
        
        // Don't process if a screen is open (let the screen handle input)
        if (minecraft.screen != null) {
            return;
        }
        
        // Check for search toggle key
        while (TOGGLE_SEARCH.consumeClick()) {
            SearchManager.toggleSearch();
        }
    }
    
    /**
     * Checks if the toggle search keybinding is pressed.
     * Useful for external checks without consuming the click.
     */
    public static boolean isToggleSearchKeyDown() {
        return TOGGLE_SEARCH.isDown();
    }
    
    /**
     * Gets the display name of the toggle search keybinding.
     * Useful for displaying the key in tooltips or instructions.
     */
    public static String getToggleSearchKeyName() {
        return TOGGLE_SEARCH.getTranslatedKeyMessage().getString();
    }
}

