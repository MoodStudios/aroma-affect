package com.ovrtechnology.keybind;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * Shared keybinding category for all Aroma Affect keybindings.
 * 
 * <p>This class centralizes the keybinding category registration to prevent
 * duplicate registration errors when multiple keybinding classes are loaded.</p>
 * 
 * <p>All Aroma Affect keybindings should use {@link #CATEGORY} as their category.</p>
 */
public final class AromaAffectKeyCategory {
    
    /**
     * The shared Aroma Affect keybinding category.
     * Registered once and shared across all keybinding classes.
     */
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, "keybinds")
    );
    
    private AromaAffectKeyCategory() {
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

