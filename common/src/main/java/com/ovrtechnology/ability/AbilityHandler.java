package com.ovrtechnology.ability;

import com.ovrtechnology.AromaAffect;
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
 * </p>
 * 
 * <h2>Registered Events:</h2>
 * <ul>
 * <li><b>RIGHT_CLICK_BLOCK</b>: Intercepts block interactions for Precise
 * Sniffer</li>
 * <li><b>SERVER_LEVEL_TICK</b>: Handles ongoing sniffing sessions</li>
 * </ul>
 * 
 * @see PreciseSnifferAbility
 */
public final class AbilityHandler {

    /**
     * Tracks players who are currently holding right-click on a suspicious block.
     * Key: Player UUID, Value: BlockPos being targeted.
     */
    private static final Map<UUID, BlockPos> ACTIVE_INTERACTIONS = new HashMap<>();

    /**
     * Whether the handler has been initialized.
     */
    private static boolean initialized = false;

    private AbilityHandler() {}

    /**
     * Initializes the ability handler and registers event listeners.
     * Should be called during mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("AbilityHandler.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing AbilityHandler...");

        // Register block interaction event
        InteractionEvent.RIGHT_CLICK_BLOCK.register(AbilityHandler::onRightClickBlock);

        // Register server tick event for processing ongoing sniffing
        TickEvent.SERVER_LEVEL_POST.register(AbilityHandler::onServerTick);

        initialized = true;
        AromaAffect.LOGGER.info("AbilityHandler initialized");
    }

    /**
     * Handles right-click block interactions.
     * 
     * <p>
     * This method intercepts interactions with Suspicious Sand when the player
     * has the Precise Sniffer ability equipped.
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
        if (!PreciseSnifferAbility.isValidTarget(block)) {
            return InteractionResult.PASS;
        }

        if (!PreciseSnifferAbility.canUse(serverPlayer)) {
            // Check if on cooldown and show feedback
            long cooldown = PreciseSnifferAbility.getRemainingCooldown(serverPlayer);
            if (cooldown > 0) {
                AromaAffect.LOGGER.debug("Player {} on cooldown for {} more ticks",
                        player.getName().getString(), cooldown);
            }
            return InteractionResult.PASS;
        }

        // Start tracking this interaction
        ACTIVE_INTERACTIONS.put(player.getUUID(), pos);

        // Process the first tick of sniffing
        boolean completed = PreciseSnifferAbility.tickSniffing(serverPlayer, pos);

        if (completed) {
            ACTIVE_INTERACTIONS.remove(player.getUUID());
        }

        // Interrupt vanilla behavior (don't use brush)
        return InteractionResult.SUCCESS;
    }

    /**
     * Handles server tick events.
     * 
     * <p>
     * This processes ongoing sniffing sessions for players who are holding
     * right-click on suspicious blocks.
     * </p>
     * 
     * @param level the server level being ticked
     */
    private static void onServerTick(Level level) {
        if (level.isClientSide()) {
            return;
        }

        // Process all active interactions
        Iterator<Map.Entry<UUID, BlockPos>> iterator = ACTIVE_INTERACTIONS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BlockPos> entry = iterator.next();
            UUID playerId = entry.getKey();
            BlockPos targetPos = entry.getValue();

            Player player = level.getPlayerByUUID(playerId);
            if (player == null || !(player instanceof ServerPlayer serverPlayer)) {
                iterator.remove();
                continue;
            }

            // Check if player is still in this level
            if (player.level() != level) {
                continue;
            }

            // Check if player is still holding right-click (using attack key as proxy)
            // Note: This is a simplification. In a real implementation, we'd track
            // the actual input state via networking or other means.
            // For now, we check if they're still looking at the same block and in range.
            if (!isStillInteracting(serverPlayer, targetPos)) {
                PreciseSnifferAbility.cancelSniffing(serverPlayer);
                iterator.remove();
                continue;
            }

            // Continue sniffing
            boolean completed = PreciseSnifferAbility.tickSniffing(serverPlayer, targetPos);

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
     * <li>Block is still a valid target</li>
     * <li>Player still has the ability equipped</li>
     * </ul>
     * 
     * @param player    the player
     * @param targetPos the target block position
     * @return true if player is likely still interacting
     */
    private static boolean isStillInteracting(ServerPlayer player, BlockPos targetPos) {
        // Check distance (player must be within 5 blocks)
        double distance = player.position().distanceTo(
                targetPos.getCenter());
        if (distance > 5.0) {
            return false;
        }

        // Check if block is still valid
        Block block = player.level().getBlockState(targetPos).getBlock();
        if (!PreciseSnifferAbility.isValidTarget(block)) {
            return false;
        }

        // Check if player still has the ability
        if (!PreciseSnifferAbility.canUse(player)) {
            return false;
        }

        return true;
    }

    /**
     * Manually stops a player's sniffing session.
     * Can be called when player releases right-click or changes tools.
     * 
     * @param player the player
     */
    public static void stopSniffing(Player player) {
        ACTIVE_INTERACTIONS.remove(player.getUUID());
        if (player instanceof ServerPlayer serverPlayer) {
            PreciseSnifferAbility.cancelSniffing(serverPlayer);
        }
    }

    /**
     * Checks if a player is currently sniffing.
     * 
     * @param player the player
     * @return true if player has an active sniffing session
     */
    public static boolean isSniffing(Player player) {
        return ACTIVE_INTERACTIONS.containsKey(player.getUUID());
    }

    /**
     * Gets the sniffing progress for a player (0.0 to 1.0).
     * 
     * @param player the player
     * @return progress or 0 if not sniffing
     */
    public static float getSniffingProgress(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return PreciseSnifferAbility.getProgress(serverPlayer);
        }
        return 0.0f;
    }
}
