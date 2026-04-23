package com.ovrtechnology.sniffer.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class SnifferLootResolver {

    private SnifferLootResolver() {}

    public static List<ItemStack> resolve(
            String noseFullId, ServerLevel level, BlockPos pos, RandomSource random) {
        List<ItemStack> drops = new ArrayList<>();
        Holder<Biome> biome = level.getBiome(pos);

        for (SnifferLootRule rule : SnifferLootRegistry.all()) {
            if (!rule.matchesNose(noseFullId)) continue;
            if (!rule.matchesBiome(biome)) continue;

            if (rule.getAlways() != null) {
                for (SnifferLootEntry entry : rule.getAlways()) {
                    resolveEntry(entry, level, pos, random, drops);
                }
            }

            SnifferLootRule.Pool pool = rule.getPool();
            if (pool != null && pool.getEntries() != null && !pool.getEntries().isEmpty()) {
                int rollCount = pool.getRolls() != null ? pool.getRolls().sample(random) : 1;
                for (int i = 0; i < rollCount; i++) {
                    SnifferLootEntry picked = pickWeighted(pool.getEntries(), random);
                    if (picked != null) {
                        resolveEntry(picked, level, pos, random, drops);
                    }
                }
            }
        }

        return drops;
    }

    private static void resolveEntry(
            SnifferLootEntry entry,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            List<ItemStack> out) {
        if (entry.getLootTable() != null && !entry.getLootTable().isEmpty()) {
            ResourceLocation tableId = ResourceLocation.tryParse(entry.getLootTable());
            if (tableId == null) return;
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, tableId);
            LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
            if (table == null) return;
            LootParams params =
                    new LootParams.Builder(level)
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                            .create(LootContextParamSets.GIFT);
            out.addAll(table.getRandomItems(params));
            return;
        }

        int count = entry.getCount() != null ? entry.getCount().sample(random) : 1;
        if (count <= 0) return;

        if (entry.getItem() != null && !entry.getItem().isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getItem());
            if (id == null) return;
            BuiltInRegistries.ITEM
                    .getOptional(id)
                    .ifPresent(item -> out.add(new ItemStack(item, count)));
            return;
        }

        if (entry.getTag() != null && !entry.getTag().isEmpty()) {
            String tagStr = entry.getTag();
            if (tagStr.startsWith("#")) tagStr = tagStr.substring(1);
            ResourceLocation tagId = ResourceLocation.tryParse(tagStr);
            if (tagId == null) return;
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            Optional<HolderSet.Named<Item>> optSet = BuiltInRegistries.ITEM.get(tagKey);
            if (optSet.isEmpty() || optSet.get().size() == 0) return;
            Optional<Holder<Item>> picked = optSet.get().getRandomElement(random);
            picked.ifPresent(h -> out.add(new ItemStack(h.value(), count)));
        }
    }

    private static SnifferLootEntry pickWeighted(
            List<SnifferLootEntry> entries, RandomSource random) {
        int totalWeight = 0;
        for (SnifferLootEntry e : entries) totalWeight += e.getWeightOrDefault();
        if (totalWeight <= 0) return null;
        int target = random.nextInt(totalWeight);
        for (SnifferLootEntry e : entries) {
            target -= e.getWeightOrDefault();
            if (target < 0) return e;
        }
        return entries.getLast();
    }
}
