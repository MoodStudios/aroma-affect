package com.ovrtechnology.ability;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import net.minecraft.world.InteractionResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Central handler for ability-related events.
 * 
 * <p>
 * This class registers event hooks using Architectury's cross-platform
 * event system to intercept player interactions and trigger abilities.
 * It delegates to registered abilities through the {@link AbilityRegistry}.
 * </p>
 * 
 * <h2>Registered Events:</h2>
 * <ul>
 * <li><b>RIGHT_CLICK_BLOCK</b>: Intercepts block interactions for block interaction abilities</li>
 * <li><b>SERVER_LEVEL_TICK</b>: Handles ongoing ability sessions</li>
 * </ul>
 * 
 * @see AbilityRegistry
 * @see BlockInteractionAbility
 */
public final class AbilityHandler {

    /**
     * Tracks players who are currently using a block interaction ability.
     * Key: Player UUID, Value: Active interaction data (block position and ability).
     */
    private static final Map<UUID, ActiveInteraction> ACTIVE_INTERACTIONS = new HashMap<>();

    /**
     * Whether the handler has been initialized.
     */
    private static boolean initialized = false;

    private AbilityHandler() {}

    /**
     * Initializes the ability handler and registers event listeners.
     * Should be called during mod initialization after abilities are registered.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("AbilityHandler.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing AbilityHandler...");

        // Register block interaction event
        InteractionEvent.RIGHT_CLICK_BLOCK.register(AbilityHandler::onRightClickBlock);

        // Register server tick event for processing ongoing interactions
        TickEvent.SERVER_LEVEL_POST.register(level -> {
            if (ReplayCompat.isReplayServer(level.getServer())) return;
            onServerTick(level);
        });

        initialized = true;
        AromaAffect.LOGGER.info("AbilityHandler initialized");
    }

    /**
     * Handles right-click block interactions.
     * 
     * <p>
     * This method checks all registered block interaction abilities and activates
     * the first one that can handle the interaction.
     * </p>
     * 
     * @param player    the player interacting
     * @param hand      the hand used
     * @param pos       the block position
     * @param direction the face of the block that was clicked
     * @return InteractionResult indicating whether to cancel vanilla behavior
     */
    private static InteractionResult onRightClickBlock(
            Player player,
            InteractionHand hand,
            BlockPos pos,
            Direction direction) {

        // Only process main hand to avoid double triggering
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (player.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        Block block = player.level().getBlockState(pos).getBlock();

        // Check all registered block interaction abilities
        for (BlockInteractionAbility ability : AbilityRegistry.getBlockInteractionAbilities()) {
            if (!ability.isValidTarget(block)) {
                continue;
            }

            if (!ability.canUse(serverPlayer)) {
                // Check if on cooldown and show feedback
                long cooldown = ability.getRemainingCooldown(serverPlayer);
                if (cooldown > 0) {
                    AromaAffect.LOGGER.debug("Player {} on cooldown for {} ({} more ticks)",
                            player.getName().getString(), ability.getId(), cooldown);
                }
                continue;
            }

            // Start tracking this interaction
            ACTIVE_INTERACTIONS.put(player.getUUID(), new ActiveInteraction(pos, ability));

            // Process the first tick
            boolean completed = ability.onInteract(serverPlayer, pos);

            if (completed) {
                ACTIVE_INTERACTIONS.remove(player.getUUID());
            }

            // Interrupt vanilla behavior
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Handles server tick events.
     * 
     * <p>
     * This processes ongoing ability sessions for players who are actively
     * using block interaction abilities.
     * </p>
     * 
     * @param level the server level being ticked
     */
    private static void onServerTick(Level level) {
        if (level.isClientSide()) {
            return;
        }

        // Process all active interactions
        Iterator<Map.Entry<UUID, ActiveInteraction>> iterator = ACTIVE_INTERACTIONS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveInteraction> entry = iterator.next();
            UUID playerId = entry.getKey();
            ActiveInteraction interaction = entry.getValue();
            BlockPos targetPos = interaction.pos();
            BlockInteractionAbility ability = interaction.ability();

            Player player = level.getPlayerByUUID(playerId);
            if (player == null || !(player instanceof ServerPlayer serverPlayer)) {
                iterator.remove();
                continue;
            }

            // Check if player is still in this level
            if (player.level() != level) {
                continue;
            }

            // Check if player is still interacting with the target block
            if (!isStillInteracting(serverPlayer, targetPos, ability)) {
                ability.onCancel(serverPlayer);
                iterator.remove();
                continue;
            }

            // Continue the ability tick
            boolean completed = ability.onTick(serverPlayer, targetPos);

            if (completed) {
                iterator.remove();
            }
        }
    }

    /**
     * Checks if a player is still interacting with the target block.
     * 
     * <p>
     * This is a heuristic check based on:
     * </p>
     * <ul>
     * <li>Player is within interaction range of the block</li>
     * <li>Block is still a valid target for the ability</li>
     * <li>Player still can use the ability</li>
     * </ul>
     * 
     * @param player    the player
     * @param targetPos the target block position
     * @param ability   the ability being used
     * @return true if player is likely still interacting
     */
    private static boolean isStillInteracting(ServerPlayer player, BlockPos targetPos, BlockInteractionAbility ability) {
        // Check distance (player must be within 5 blocks)
        double distance = player.position().distanceTo(targetPos.getCenter());
        if (distance > 5.0) {
            return false;
        }

        // Check if block is still valid for this ability
        Block block = player.level().getBlockState(targetPos).getBlock();
        if (!ability.isValidTarget(block)) {
            return false;
        }

        // Check if player still can use the ability
        if (!ability.canUse(player)) {
            return false;
        }

        return true;
    }

    /**
     * Manually stops a player's active ability interaction.
     * Can be called when player releases right-click or changes tools.
     * 
     * @param player the player
     */
    public static void stopInteraction(Player player) {
        ActiveInteraction removed = ACTIVE_INTERACTIONS.remove(player.getUUID());
        if (removed != null && player instanceof ServerPlayer serverPlayer) {
            removed.ability().onCancel(serverPlayer);
        }
    }

    /**
     * Checks if a player has an active ability interaction.
     * 
     * @param player the player
     * @return true if player has an active interaction
     */
    public static boolean hasActiveInteraction(Player player) {
        return ACTIVE_INTERACTIONS.containsKey(player.getUUID());
    }

    /**
     * Gets the active ability for a player, if any.
     * 
     * @param player the player
     * @return the active ability, or null if none
     */
    public static BlockInteractionAbility getActiveAbility(Player player) {
        ActiveInteraction interaction = ACTIVE_INTERACTIONS.get(player.getUUID());
        return interaction != null ? interaction.ability() : null;
    }

    /**
     * Gets the interaction progress for a player (0.0 to 1.0).
     * 
     * @param player the player
     * @return progress or 0 if no active interaction
     */
    public static float getInteractionProgress(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ActiveInteraction interaction = ACTIVE_INTERACTIONS.get(player.getUUID());
            if (interaction != null) {
                return interaction.ability().getProgress(serverPlayer);
            }
        }
        return 0.0f;
    }

    /**
     * Data class representing an active ability interaction.
     * 
     * @param pos     the block position being interacted with
     * @param ability the ability being used
     */
    private record ActiveInteraction(BlockPos pos, BlockInteractionAbility ability) {}
}
