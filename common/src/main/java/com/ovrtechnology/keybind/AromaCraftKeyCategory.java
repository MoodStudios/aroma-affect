package com.ovrtechnology.keybind;

import com.ovrtechnology.AromaCraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared keybinding category for all AromaCraft keybindings.
 * 
 * <p>This class centralizes the keybinding category registration to prevent
 * duplicate registration errors when multiple keybinding classes are loaded.</p>
 * 
 * <p>All AromaCraft keybindings should use {@link #CATEGORY} as their category.</p>
 */
public final class AromaCraftKeyCategory {
    
    /**
     * The shared AromaCraft keybinding category.
     * Registered once and shared across all keybinding classes.
     */
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            ResourceLocation.fromNamespaceAndPath(AromaCraft.MOD_ID, "keybinds")
    );
    
    private AromaCraftKeyCategory() {
        // Utility class
    }
    
    /**
     * Forces static initialization of this class.
     * Call this early in mod initialization to ensure the category is registered
     * before any keybinding classes are loaded.
     */
    public static void ensureInitialized() {
        // Accessing CATEGORY forces the static initializer to run
        // This is a no-op method but ensures the class is loaded
    }
}

