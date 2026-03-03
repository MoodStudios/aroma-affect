package com.ovrtechnology.tutorial.trade;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.tutorial.animation.TutorialAnimationHandler;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles the execution of Oliver trades.
 * <p>
 * Validates all required input items in the player's inventory,
 * removes them, and gives the output items (dropping if inventory is full).
 * Supports multiple different input items per trade.
 */
public final class TutorialTradeHandler {

    private TutorialTradeHandler() {
    }

    /**
     * Result of a trade attempt with detailed info about missing items.
     */
    public record TradeResult(boolean success, String message) {}

    /**
     * Executes a trade for a player and returns detailed result.
     *
     * @param player  the server player
     * @param level   the server level
     * @param tradeId the trade ID
     * @return TradeResult with success status and message
     */
    public static TradeResult executeTradeWithResult(ServerPlayer player, ServerLevel level, String tradeId) {
        AromaAffect.LOGGER.info("Trade request: tradeId='{}', dimension='{}', player='{}'",
                tradeId, level.dimension().location(), player.getName().getString());

        Optional<TutorialTrade> tradeOpt = TutorialTradeManager.getTrade(level, tradeId);
        if (tradeOpt.isEmpty()) {
            AromaAffect.LOGGER.warn("Trade {} not found in dimension {}", tradeId, level.dimension().location());
            return new TradeResult(false, "Trade not found");
        }

        TutorialTrade trade = tradeOpt.get();
        AromaAffect.LOGGER.info("Trade found: id='{}', hasInputs={}, hasOutput={}, isComplete={}",
                trade.getId(), trade.hasInputs(), trade.hasOutput(), trade.isComplete());

        if (!trade.isComplete()) {
            AromaAffect.LOGGER.warn("Trade {} is incomplete (missing inputs or output)", tradeId);
            return new TradeResult(false, "No trade available");
        }

        // Resolve and validate ALL input items first, tracking missing items
        List<ResolvedInput> resolvedInputs = new ArrayList<>();
        List<String> missingItems = new ArrayList<>();

        for (TutorialTrade.InputEntry entry : trade.getInputs()) {
            Optional<Item> itemOpt = BuiltInRegistries.ITEM
                    .getOptional(ResourceLocation.parse(entry.itemId()));
            if (itemOpt.isEmpty()) {
                AromaAffect.LOGGER.warn("Trade {} input item not found: {}", tradeId, entry.itemId());
                return new TradeResult(false, "Invalid trade item: " + entry.itemId());
            }

            Item item = itemOpt.get();
            int found = countItemInInventory(player, item);
            if (found < entry.count()) {
                // Track what's missing
                String itemName = getItemDisplayName(entry.itemId());
                int missing = entry.count() - found;
                missingItems.add(itemName + " (" + found + "/" + entry.count() + ")");
            }

            resolvedInputs.add(new ResolvedInput(item, entry.count(), found));
        }

        // If any items are missing, return detailed message
        if (!missingItems.isEmpty()) {
            String message = "Missing: " + String.join(", ", missingItems);
            return new TradeResult(false, message);
        }

        // All checks passed — remove all input items
        for (ResolvedInput input : resolvedInputs) {
            removeItemsFromInventory(player, input.item, input.required);
        }

        // Resolve output item
        Optional<Item> outputItemOpt = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.parse(trade.getOutputItemId()));
        if (outputItemOpt.isEmpty()) {
            AromaAffect.LOGGER.warn("Trade {} output item not found: {}", tradeId, trade.getOutputItemId());
            return new TradeResult(false, "Invalid output item");
        }

        // Give output items to player
        ItemStack outputStack = new ItemStack(outputItemOpt.get(), trade.getOutputCount());
        if (!player.getInventory().add(outputStack)) {
            // Drop remaining items at player's feet
            player.drop(outputStack, false);
        }

        // Play success sound
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.VILLAGER_TRADE,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F
        );

        AromaAffect.LOGGER.debug("Trade {} executed for player {}", tradeId, player.getName().getString());

        // Execute onComplete actions
        executeOnCompleteActions(player, level, trade);

        return new TradeResult(true, "Trade successful!");
    }

    /**
     * Executes a trade for a player (simple boolean version).
     *
     * @param player  the server player
     * @param level   the server level
     * @param tradeId the trade ID
     * @return true if the trade succeeded, false otherwise
     */
    public static boolean executeTrade(ServerPlayer player, ServerLevel level, String tradeId) {
        return executeTradeWithResult(player, level, tradeId).success();
    }

    /**
     * Gets a display-friendly name for an item ID.
     */
    private static String getItemDisplayName(String itemId) {
        // Remove namespace and convert underscores to spaces
        String name = itemId;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(':') + 1);
        }
        // Capitalize first letter and replace underscores
        name = name.replace('_', ' ');
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    /**
     * Executes the onComplete actions for a trade.
     */
    private static void executeOnCompleteActions(ServerPlayer player, ServerLevel level, TutorialTrade trade) {
        // Execute Oliver action
        if (trade.hasOnCompleteOliverAction()) {
            executeOliverAction(player, level, trade.getOnCompleteOliverAction());
        }

        // Start cinematic
        if (trade.hasOnCompleteCinematic()) {
            TutorialCinematicHandler.startCinematic(player, trade.getOnCompleteCinematicId());
            AromaAffect.LOGGER.debug("Trade {} triggered cinematic {}", trade.getId(), trade.getOnCompleteCinematicId());
        }

        // Play animation
        if (trade.hasOnCompleteAnimation()) {
            TutorialAnimationHandler.play(level, trade.getOnCompleteAnimationId());
            AromaAffect.LOGGER.debug("Trade {} triggered animation {}", trade.getId(), trade.getOnCompleteAnimationId());
        }

        // Activate waypoint
        if (trade.hasOnCompleteWaypoint()) {
            String waypointId = trade.getOnCompleteWaypointId();
            Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, waypointId);
            if (wpOpt.isPresent() && wpOpt.get().isComplete()) {
                TutorialWaypoint wp = wpOpt.get();
                TutorialWaypointAreaHandler.setActiveWaypoint(player.getUUID(), waypointId);
                TutorialWaypointNetworking.sendWaypointToPlayer(player, waypointId, wp.getValidPositions());
                AromaAffect.LOGGER.debug("Trade {} triggered waypoint {}", trade.getId(), waypointId);
            } else {
                AromaAffect.LOGGER.warn("Trade {} onComplete waypoint {} not found or incomplete", trade.getId(), waypointId);
            }
        }
    }

    /**
     * Executes Oliver action(s) for a player.
     * Multiple actions can be separated by semicolon (;).
     */
    private static void executeOliverAction(ServerPlayer player, ServerLevel level, String action) {
        TutorialOliverEntity oliver = findNearestOliver(player, level);
        if (oliver == null) {
            AromaAffect.LOGGER.warn("Cannot execute Oliver action '{}': no Oliver found nearby", action);
            return;
        }

        // Support multiple actions separated by semicolon
        String[] actions = action.split(";");
        String pendingDialogueId = null;

        for (String singleAction : actions) {
            singleAction = singleAction.trim();
            if (singleAction.isEmpty()) continue;

            String actionLower = singleAction.toLowerCase();

            if (actionLower.equals("follow")) {
                oliver.setFollowing(player);
            } else if (actionLower.equals("stop")) {
                oliver.setStationary();
            } else if (actionLower.startsWith("walkto:")) {
                String coordsStr = singleAction.substring(7);
                String[] parts = coordsStr.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());
                        oliver.setWalkingTo(new BlockPos(x, y, z));
                    } catch (NumberFormatException e) {
                        AromaAffect.LOGGER.warn("Invalid walkto coordinates: {}", coordsStr);
                    }
                }
            } else if (actionLower.startsWith("dialogue:")) {
                // Defer dialogue opening until after all other actions
                pendingDialogueId = singleAction.substring(9).trim();
                oliver.setDialogueId(pendingDialogueId);
            } else if (actionLower.startsWith("trade:")) {
                String tradeId = singleAction.substring(6).trim();
                oliver.setTradeId(tradeId);
            } else if (actionLower.equals("cleartrade")) {
                oliver.setTradeId("");
            } else if (actionLower.startsWith("teleportplayer:")) {
                String coordsStr = singleAction.substring(15);
                String[] parts = coordsStr.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());
                        player.teleportTo(level, x + 0.5, y, z + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
                        AromaAffect.LOGGER.debug("Teleported player to {}, {}, {}", x, y, z);
                    } catch (NumberFormatException e) {
                        AromaAffect.LOGGER.warn("Invalid teleportplayer coordinates: {}", coordsStr);
                    }
                }
            }

            AromaAffect.LOGGER.debug("Executed Oliver action '{}' for trade completion", singleAction);
        }

        // Open dialogue AFTER processing all actions (so trade is set first)
        if (pendingDialogueId != null) {
            TutorialDialogueContentNetworking.sendOpenDialogue(
                    player, oliver.getId(), pendingDialogueId,
                    oliver.hasTrade(), oliver.getTradeId()
            );
        }
    }

    /**
     * Finds the nearest Oliver entity to a player.
     */
    private static TutorialOliverEntity findNearestOliver(ServerPlayer player, ServerLevel level) {
        AABB searchArea = new AABB(
                player.getX() - 100, player.getY() - 50, player.getZ() - 100,
                player.getX() + 100, player.getY() + 50, player.getZ() + 100
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class,
                searchArea
        );

        if (olivers.isEmpty()) {
            return null;
        }

        TutorialOliverEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (TutorialOliverEntity oliver : olivers) {
            double dist = oliver.distanceToSqr(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = oliver;
            }
        }

        return nearest;
    }

    private static int countItemInInventory(ServerPlayer player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItemsFromInventory(ServerPlayer player, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    private record ResolvedInput(Item item, int required, int found) {}
}
