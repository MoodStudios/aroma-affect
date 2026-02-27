package com.ovrtechnology.tutorial.chest;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialChestNetworking;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Handles interaction with tutorial chests.
 * <p>
 * When a player right-clicks a marked chest position:
 * <ul>
 *   <li>Gives all reward items to the player</li>
 *   <li>Activates waypoint if configured</li>
 *   <li>Activates cinematic if configured</li>
 *   <li>Marks the chest as consumed</li>
 * </ul>
 */
public final class TutorialChestHandler {

    private static boolean initialized = false;

    private TutorialChestHandler() {
    }

    /**
     * Initializes the chest handler.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            if (!(serverPlayer.level() instanceof ServerLevel level)) {
                return InteractionResult.PASS;
            }

            // Check if tutorial is active
            if (!TutorialModule.isActive(level)) {
                return InteractionResult.PASS;
            }

            // Check if there's a chest at this position
            Optional<TutorialChest> chestOpt = TutorialChestManager.getChestAt(level, pos);
            if (chestOpt.isEmpty()) {
                return InteractionResult.PASS;
            }

            TutorialChest chest = chestOpt.get();

            // Check if already consumed
            if (chest.isConsumed()) {
                return InteractionResult.PASS;
            }

            // Check if the block is actually a chest
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST) &&
                !state.is(Blocks.BARREL) && !state.is(Blocks.ENDER_CHEST)) {
                // Not a chest-like block, skip
                return InteractionResult.PASS;
            }

            // Process the chest - give items directly without opening the chest GUI
            onChestOpened(serverPlayer, level, chest);

            // Consume the event to prevent the chest from opening
            return InteractionResult.SUCCESS;
        });

        AromaAffect.LOGGER.debug("Tutorial chest handler initialized");
    }

    /**
     * Called when a player opens a tutorial chest.
     */
    private static void onChestOpened(ServerPlayer player, ServerLevel level, TutorialChest chest) {
        String chestId = chest.getId();

        // Give rewards
        if (chest.hasRewards()) {
            for (ItemStack reward : chest.getRewards()) {
                ItemStack copy = reward.copy();
                if (!player.getInventory().add(copy)) {
                    // Inventory full, drop on ground
                    player.drop(copy, false);
                }
            }

            // Play chest open + item pickup sounds
            level.playSound(
                    null,
                    chest.getPosition().getX() + 0.5,
                    chest.getPosition().getY() + 0.5,
                    chest.getPosition().getZ() + 0.5,
                    SoundEvents.CHEST_OPEN,
                    SoundSource.BLOCKS,
                    0.8f,
                    1.0f
            );
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f
            );

            AromaAffect.LOGGER.debug("Gave {} rewards to player {} from chest {}",
                    chest.getRewardCount(), player.getName().getString(), chestId);
        }

        // Mark as consumed
        TutorialChestManager.consumeChest(level, chestId);

        // Sync to client (removes particle effect)
        TutorialChestNetworking.sendChestConsumed(player, chest.getPosition());

        // Activate cinematic if configured (before waypoint, as cinematic might control waypoint)
        if (chest.hasActivateCinematic()) {
            String cinematicId = chest.getActivateCinematicId();
            TutorialCinematicHandler.startCinematic(player, cinematicId);
            AromaAffect.LOGGER.debug("Activated cinematic {} from chest {}", cinematicId, chestId);
        }

        // Activate waypoint if configured (and no cinematic, or cinematic will handle it)
        if (chest.hasActivateWaypoint() && !chest.hasActivateCinematic()) {
            String waypointId = chest.getActivateWaypointId();
            Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, waypointId);
            if (wpOpt.isPresent() && wpOpt.get().isComplete()) {
                TutorialWaypoint wp = wpOpt.get();
                TutorialWaypointAreaHandler.setActiveWaypoint(player.getUUID(), waypointId);
                TutorialWaypointNetworking.sendWaypointToPlayer(player, waypointId, wp.getValidPositions());
                AromaAffect.LOGGER.debug("Activated waypoint {} from chest {}", waypointId, chestId);
            } else {
                AromaAffect.LOGGER.warn("Waypoint {} from chest {} not found or incomplete", waypointId, chestId);
            }
        }
    }
}
