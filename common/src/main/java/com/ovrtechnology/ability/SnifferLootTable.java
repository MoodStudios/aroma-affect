package com.ovrtechnology.ability;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class SnifferLootTable {

    public static final int SNIFFER_EGG_CHANCE = 40;

    private SnifferLootTable() {}

    public static ItemStack rollLoot(
            ServerLevel level,
            BlockPos pos,
            ResourceKey<LootTable> vanillaLootKey,
            ServerPlayer player,
            RandomSource random) {

        return rollLoot(level, pos, vanillaLootKey, player, random, SNIFFER_EGG_CHANCE / 100.0);
    }

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

    private static ItemStack rollVanillaLoot(
            ServerLevel level,
            BlockPos pos,
            ResourceKey<LootTable> lootKey,
            ServerPlayer player,
            RandomSource random) {

        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootKey);

        ItemStack tool = player.getItemBySlot(EquipmentSlot.HEAD);

        LootParams lootParams =
                new LootParams.Builder(level)
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

    public static int getSnifferEggChance() {

        return SNIFFER_EGG_CHANCE;
    }
}
