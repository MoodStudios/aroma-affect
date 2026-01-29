package com.ovrtechnology.trigger;

/**
 * Defines the sources that can trigger a scent emission.
 * 
 * <p>Each source represents a different type of game event or interaction
 * that can activate a scent on the OVR hardware.</p>
 */
public enum ScentTriggerSource {
    
    /**
     * Player uses a scent item (right-click).
     * This is an explicit, intentional trigger by the player.
     */
    ITEM_USE,
    
    /**
     * Player holds a scent item in hand.
     * Passive trigger while item is equipped.
     * (Future implementation)
     */
    ITEM_HOLD,
    
    /**
     * Player enters a new biome.
     * One-time trigger on biome transition.
     * (Future implementation)
     */
    BIOME_ENTER,
    
    /**
     * Continuous ambient scent from current biome.
     * Low-priority background scent.
     * (Future implementation)
     */
    BIOME_AMBIENT,
    
    /**
     * Player interacts with a specific block.
     * Triggered on block right-click or break.
     * (Future implementation)
     */
    BLOCK_INTERACT,
    
    /**
     * Player is near a specific block type.
     * Proximity-based trigger within a range.
     * (Future implementation)
     */
    BLOCK_PROXIMITY,
    
    /**
     * Player is near a specific mob type.
     * Proximity-based trigger within a range.
     * (Future implementation)
     */
    MOB_PROXIMITY,
    
    /**
     * Custom game events (explosions, weather, etc.).
     * Catch-all for special triggers.
     * (Future implementation)
     */
    CUSTOM_EVENT,
    
    /**
     * Passive mode - automatic scent emission when near blocks/biomes/structures.
     * Only works when OVR hardware is connected and player doesn't have a nose equipped.
     */
    PASSIVE_MODE,

    /**
     * Path tracking mode - scent emission while following a path to a target.
     * Triggered periodically while the player has an active path to a block/biome/structure.
     */
    PATH_TRACKING
}
