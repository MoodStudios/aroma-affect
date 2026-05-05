package com.ovrtechnology.search;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.nose.NoseItem;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Manages the search/tracking state for Aroma Affect.
 * 
 * <p>This class handles the activation and deactivation of the scent search mode,
 * which allows players to track targets when they have a Nose equipped.</p>
 * 
 * <h3>Search States:</h3>
 * <ul>
 *   <li><b>Inactive</b>: Default state, no active tracking</li>
 *   <li><b>Active</b>: Player is actively searching/tracking targets</li>
 * </ul>
 * 
 * <h3>Preconditions for Activation:</h3>
 * <ul>
 *   <li>Player must have a Nose item equipped in the head slot</li>
 *   <li>Player must be in a valid game state (not in menu, etc.)</li>
 * </ul>
 */
public final class SearchManager {
    
    /**
     * Whether the search mode is currently active.
     */
    @Getter
    private static boolean searchActive = false;
    
    /**
     * Whether the manager has been initialized.
     */
    private static boolean initialized = false;
    
    private SearchManager() {
        // Utility class
    }
    
    /**
     * Initializes the search manager.
     * Should be called during client-side mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("SearchManager.init() called multiple times!");
            return;
        }
        
        AromaAffect.LOGGER.info("Initializing SearchManager...");
        
        // Reset state
        searchActive = false;
        
        initialized = true;
        AromaAffect.LOGGER.info("SearchManager initialized");
    }
    
    /**
     * Toggles the search mode on/off.
     * 
     * <p>This will validate that the player has a Nose equipped before activating.
     * If no nose is equipped, the search will not activate and a message will be logged.</p>
     * 
     * @return true if the toggle was successful, false if preconditions were not met
     */
    public static boolean toggleSearch() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player == null) {
            AromaAffect.LOGGER.debug("Cannot toggle search: No player instance");
            return false;
        }
        
        // Check if nose is equipped
        if (!isNoseEquipped(player)) {
            AromaAffect.LOGGER.info("[Aroma Affect Search] Cannot activate search - No Nose equipped!");
            return false;
        }
        
        // Toggle the state
        searchActive = !searchActive;
        
        if (searchActive) {
            onSearchActivated(player);
        } else {
            onSearchDeactivated(player);
        }
        
        return true;
    }
    
    /**
     * Activates the search mode.
     * Does nothing if already active or preconditions are not met.
     * 
     * @return true if activation was successful
     */
    public static boolean activateSearch() {
        if (searchActive) {
            return true; // Already active
        }
        return toggleSearch();
    }
    
    /**
     * Deactivates the search mode.
     * Does nothing if already inactive.
     */
    public static void deactivateSearch() {
        if (!searchActive) {
            return; // Already inactive
        }
        
        searchActive = false;
        
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            onSearchDeactivated(player);
        }
    }
    
    /**
     * Called when search mode is activated.
     */
    private static void onSearchActivated(Player player) {
        ItemStack noseStack = com.ovrtechnology.nose.accessory.NoseAccessory.getEquipped(player);
        String noseName = "Unknown";
        
        if (noseStack.getItem() instanceof NoseItem noseItem) {
            noseName = noseItem.getDefinition().getId();
        }
        
        AromaAffect.LOGGER.info("[Aroma Affect Search] Search mode ACTIVATED with: {}", noseName);
        
        // TODO: Future implementation
        // - Start visual effects (particles, HUD overlay)
        // - Initialize tracking target if one is selected
        // - Send scent activation to OVR hardware bridge
    }
    
    /**
     * Called when search mode is deactivated.
     */
    private static void onSearchDeactivated(Player player) {
        AromaAffect.LOGGER.info("[Aroma Affect Search] Search mode DEACTIVATED");
        
        // TODO: Future implementation
        // - Stop visual effects
        // - Clear tracking state
        // - Stop scent emission to OVR hardware
    }
    
    /**
     * Checks if the player has a Nose item equipped in the head slot.
     * 
     * @param player The player to check
     * @return true if a Nose is equipped
     */
    public static boolean isNoseEquipped(Player player) {
        if (player == null) {
            return false;
        }
        
        ItemStack headStack = com.ovrtechnology.nose.accessory.NoseAccessory.getEquipped(player);
        return !headStack.isEmpty() && headStack.getItem() instanceof NoseItem;
    }
    
    /**
     * Gets the currently equipped Nose item, if any.
     * 
     * @param player The player to check
     * @return The NoseItem if equipped, or null if no nose is equipped
     */
    public static NoseItem getEquippedNose(Player player) {
        if (player == null) {
            return null;
        }
        
        ItemStack headStack = com.ovrtechnology.nose.accessory.NoseAccessory.getEquipped(player);
        if (!headStack.isEmpty() && headStack.getItem() instanceof NoseItem noseItem) {
            return noseItem;
        }
        
        return null;
    }
    
    /**
     * Checks if search is currently active and valid.
     * This validates that the player still has a nose equipped.
     * If the nose was removed, the search will be automatically deactivated.
     * 
     * @return true if search is active and valid
     */
    public static boolean isSearchActiveAndValid() {
        if (!searchActive) {
            return false;
        }
        
        Player player = Minecraft.getInstance().player;
        if (player == null || !isNoseEquipped(player)) {
            // Nose was unequipped, deactivate search
            searchActive = false;
            AromaAffect.LOGGER.info("[Aroma Affect Search] Search auto-deactivated: Nose unequipped");
            return false;
        }
        
        return true;
    }
}

