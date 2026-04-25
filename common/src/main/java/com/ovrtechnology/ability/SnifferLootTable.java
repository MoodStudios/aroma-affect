package com.ovrtechnology.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Loot resolver for the Precise Sniffer ability.
 * 
 * <p>
 * This class handles loot generation when a player with the Precise Sniffer
 * ability interacts with Suspicious Sand. It provides a chance to obtain
 * Sniffer Eggs, falling back to the vanilla archaeology loot table otherwise.
 * </p>
 * 
 * <p>
 * Logic:
 * </p>
 * <ul>
 * <li>Roll a random number from 0 to 100</li>
 * <li>If roll <= SNIFFER_EGG_CHANCE (40%), return Sniffer Egg</li>
 * <li>Otherwise, use the vanilla archaeology loot table</li>
 * </ul>
 * 
 * @see PreciseSnifferAbility
 */
public final class SnifferLootTable {

    public static final int SNIFFER_EGG_CHANCE = 40;

    private SnifferLootTable() {}

    /**
     * Rolls loot for the Precise Sniffer ability using the default chance.
     * 
     * <p>
     * First checks if the player wins a Sniffer Egg (40% chance).
     * If not, delegates to the vanilla archaeology loot table.
     * </p>
     * 
     * @param level          the server level
     * @param pos            the position of the suspicious sand block
     * @param vanillaLootKey the vanilla loot table key from the brushable block
     * @param player         the player using the ability (required for loot
     *                       context)
     * @param random         the random source
     * @return the rolled ItemStack (Sniffer Egg or vanilla loot)
     */
    public static ItemStack rollLoot(
            ServerLevel level,
            BlockPos pos,
            ResourceKey<LootTable> vanillaLootKey,
            ServerPlayer player,
            RandomSource random) {

        return rollLoot(level, pos, vanillaLootKey, player, random, SNIFFER_EGG_CHANCE / 100.0);
    }

    /**
     * Rolls loot for the Precise Sniffer ability with a custom Sniffer Egg chance.
     * 
     * <p>
     * First checks if the player wins a Sniffer Egg based on the provided chance.
     * If not, delegates to the vanilla archaeology loot table.
     * </p>
     * 
     * @param level            the server level
     * @param pos              the position of the suspicious sand block
     * @param vanillaLootKey   the vanilla loot table key from the brushable block
     * @param player           the player using the ability (required for loot context)
     * @param random           the random source
     * @param snifferEggChance the chance (0.0 to 1.0) to get a Sniffer Egg
     * @return the rolled ItemStack (Sniffer Egg or vanilla loot)
     */
    public static ItemStack rollLoot(
            ServerLevel level,
            BlockPos pos,
            ResourceKey<LootTable> vanillaLootKey,
            ServerPlayer player,
            RandomSource random,
            double snifferEggChance) {

        if (random.nextDouble() < snifferEggChance) {
            return new ItemStack(Items.SNIFFER_EGG);
        }

        return rollVanillaLoot(level, pos, vanillaLootKey, player, random);
    }

    /**
     * Rolls loot from the vanilla archaeology loot table.
     * 
     * @param level   the server level
     * @param pos     the block position (used for loot context)
     * @param lootKey the loot table resource key
     * @param player  the player (required for ARCHAEOLOGY loot context)
     * @param random  the random source
     * @return the rolled ItemStack, or empty if loot table not found
     */
    private static ItemStack rollVanillaLoot(
            ServerLevel level,
            BlockPos pos,
            ResourceKey<LootTable> lootKey,
            ServerPlayer player,
            RandomSource random) {

        LootTable lootTable = level.getServer().reloadableRegistries()
                .getLootTable(lootKey);

        ItemStack tool = com.ovrtechnology.nose.accessory.NoseAccessory.getEquipped(player);

        LootParams lootParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.ARCHAEOLOGY);

        List<ItemStack> loot = lootTable.getRandomItems(lootParams, random);

        if (!loot.isEmpty()) {
            return loot.getFirst();
        }

        return ItemStack.EMPTY;
    }

    /**
     * Simple check if a roll should give a Sniffer Egg.
     * Useful for client-side prediction or simple checks.
     * 
     * @param random the random source
     * @return true if this roll should give a Sniffer Egg
     */
    public static boolean shouldGiveSnifferEgg(RandomSource random) {
        return random.nextInt(100) < SNIFFER_EGG_CHANCE;
    }

    /**
     * Gets the current Sniffer Egg chance percentage.
     * 
     * @return the chance (0-100)
     */
    public static int getSnifferEggChance() {
        // TODO: Read from config when config system is implemented
        return SNIFFER_EGG_CHANCE;
    }
}
