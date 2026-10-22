package com.ovrtechnology.ability;

import net.minecraft.server.level.ServerPlayer;

/**
 * Base interface for all abilities in Aroma Affect.
 * 
 * <p>
 * Abilities are special actions that can be performed by players
 * when wearing a nose with the corresponding ability unlocked.
 * </p>
 * 
 * <p>
 * Each ability has a unique ID that must match the strings defined
 * in the nose JSON configuration files and {@link AbilityConstants}.
 * </p>
 * 
 * @see AbilityConstants
 * @see AbilityRegistry
 */
public interface Ability {

    /**
     * Gets the unique identifier for this ability.
     * 
     * <p>
     * This ID must match the ability strings defined in the nose JSON
     * configuration files (data/aromaaffect/noses/noses.json).
     * </p>
     * 
     * @return the ability ID (e.g., "precise_sniffer")
     */
    String getId();

    /**
     * Checks if a player can currently use this ability.
     * 
     * <p>
     * This typically involves checking:
     * </p>
     * <ul>
     * <li>Player has a nose equipped with this ability</li>
     * <li>Ability is not on cooldown</li>
     * <li>Any other ability-specific requirements</li>
     * </ul>
     * 
     * @param player the server player to check
     * @return true if the player can use this ability
     */
    boolean canUse(ServerPlayer player);
}
