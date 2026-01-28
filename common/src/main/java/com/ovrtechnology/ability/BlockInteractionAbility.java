package com.ovrtechnology.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

/**
 * Interface for abilities that activate through block interactions.
 * 
 * <p>
 * Block interaction abilities are triggered when a player right-clicks
 * on a valid target block while having the ability equipped. These abilities
 * typically have:
 * </p>
 * <ul>
 * <li>A progress-based activation (like brushing)</li>
 * <li>Cooldowns after successful use</li>
 * <li>Cancellation when conditions are no longer met</li>
 * </ul>
 * 
 * <h2>Lifecycle:</h2>
 * <ol>
 * <li>{@link #isValidTarget(Block)} - Check if clicked block is valid</li>
 * <li>{@link #canUse(ServerPlayer)} - Check if player can use ability</li>
 * <li>{@link #onInteract(ServerPlayer, BlockPos)} - Start interaction</li>
 * <li>{@link #onTick(ServerPlayer, BlockPos)} - Process each tick</li>
 * <li>{@link #onCancel(ServerPlayer)} - Handle cancellation if needed</li>
 * </ol>
 * 
 * @see Ability
 * @see AbilityHandler
 */
public interface BlockInteractionAbility extends Ability {

    /**
     * Checks if a block is a valid target for this ability.
     * 
     * @param block the block to check
     * @return true if this ability can be used on the block
     */
    boolean isValidTarget(Block block);

    /**
     * Called when the player first interacts with a valid target block.
     * 
     * <p>
     * This method should initialize any necessary state for the interaction
     * and process the first tick of the ability.
     * </p>
     * 
     * @param player the server player interacting
     * @param pos    the position of the target block
     * @return true if the interaction completed this tick, false if ongoing
     */
    boolean onInteract(ServerPlayer player, BlockPos pos);

    /**
     * Called when an active interaction should be cancelled.
     * 
     * <p>
     * This is called when:
     * </p>
     * <ul>
     * <li>Player moves too far from the target</li>
     * <li>Target block is no longer valid</li>
     * <li>Player no longer has the ability equipped</li>
     * <li>Player manually cancels the action</li>
     * </ul>
     * 
     * @param player the server player
     */
    void onCancel(ServerPlayer player);

    /**
     * Called each server tick while the interaction is active.
     * 
     * <p>
     * This method should update progress, play sounds/particles,
     * and check for completion.
     * </p>
     * 
     * @param player the server player
     * @param pos    the position of the target block
     * @return true if the interaction completed this tick, false if ongoing
     */
    boolean onTick(ServerPlayer player, BlockPos pos);

    /**
     * Checks if the player has an active interaction with this ability.
     * 
     * @param player the server player
     * @return true if the player is currently using this ability
     */
    boolean isActive(ServerPlayer player);

    /**
     * Gets the current progress of the interaction (0.0 to 1.0).
     * 
     * @param player the server player
     * @return progress from 0.0 (just started) to 1.0 (complete), or 0 if not active
     */
    float getProgress(ServerPlayer player);

    /**
     * Gets the remaining cooldown time in ticks.
     * 
     * @param player the server player
     * @return remaining ticks until ability can be used again, or 0 if ready
     */
    long getRemainingCooldown(ServerPlayer player);
}
