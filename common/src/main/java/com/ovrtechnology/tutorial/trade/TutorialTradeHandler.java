package com.ovrtechnology.tutorial.trade;

import com.ovrtechnology.AromaAffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
     * Executes a trade for a player.
     *
     * @param player  the server player
     * @param level   the server level
     * @param tradeId the trade ID
     * @return true if the trade succeeded, false otherwise
     */
    public static boolean executeTrade(ServerPlayer player, ServerLevel level, String tradeId) {
        Optional<TutorialTrade> tradeOpt = TutorialTradeManager.getTrade(level, tradeId);
        if (tradeOpt.isEmpty()) {
            AromaAffect.LOGGER.warn("Trade {} not found", tradeId);
            return false;
        }

        TutorialTrade trade = tradeOpt.get();
        if (!trade.isComplete()) {
            AromaAffect.LOGGER.warn("Trade {} is incomplete (missing inputs or output)", tradeId);
            return false;
        }

        // Resolve and validate ALL input items first
        List<ResolvedInput> resolvedInputs = new ArrayList<>();
        for (TutorialTrade.InputEntry entry : trade.getInputs()) {
            Optional<Item> itemOpt = BuiltInRegistries.ITEM
                    .getOptional(ResourceLocation.parse(entry.itemId()));
            if (itemOpt.isEmpty()) {
                AromaAffect.LOGGER.warn("Trade {} input item not found: {}", tradeId, entry.itemId());
                return false;
            }

            Item item = itemOpt.get();
            int found = countItemInInventory(player, item);
            if (found < entry.count()) {
                // Player doesn't have enough of this item
                return false;
            }

            resolvedInputs.add(new ResolvedInput(item, entry.count()));
        }

        // All checks passed — remove all input items
        for (ResolvedInput input : resolvedInputs) {
            removeItemsFromInventory(player, input.item, input.count);
        }

        // Resolve output item
        Optional<Item> outputItemOpt = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.parse(trade.getOutputItemId()));
        if (outputItemOpt.isEmpty()) {
            AromaAffect.LOGGER.warn("Trade {} output item not found: {}", tradeId, trade.getOutputItemId());
            return false;
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
        return true;
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

    private record ResolvedInput(Item item, int count) {}
}
