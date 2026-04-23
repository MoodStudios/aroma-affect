package com.ovrtechnology.lookup.strategy;

import com.mojang.datafixers.util.Pair;
import com.ovrtechnology.lookup.LookupResult;
import com.ovrtechnology.lookup.LookupTarget;
import com.ovrtechnology.lookup.LookupType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

public class BiomeLookupStrategy implements LookupStrategy {

    private static final int DEFAULT_RADIUS = 6400;

    private static final int MAX_RADIUS = 32000;

    private static final int HORIZONTAL_STEP = 32;

    private static final int VERTICAL_STEP = 64;

    @Override
    public LookupResult lookup(
            ServerLevel level, BlockPos origin, LookupTarget target, int maxRadius) {
        if (target.type() != LookupType.BIOME) {
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.INVALID_TARGET);
        }

        long startTime = System.currentTimeMillis();

        try {

            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, target.resourceId());

            Pair<BlockPos, Holder<Biome>> result =
                    level.findClosestBiome3d(
                            holder -> holder.is(biomeKey),
                            origin,
                            Math.min(maxRadius, MAX_RADIUS),
                            HORIZONTAL_STEP,
                            VERTICAL_STEP);

            long searchTime = System.currentTimeMillis() - startTime;

            if (result != null) {
                return LookupResult.success(
                        target, level.dimension(), origin, result.getFirst(), searchTime, false);
            } else {
                return LookupResult.notFound(target, level.dimension(), origin, searchTime);
            }
        } catch (Exception e) {
            long searchTime = System.currentTimeMillis() - startTime;
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    searchTime,
                    LookupResult.FailureReason.ERROR);
        }
    }

    @Override
    public int getDefaultRadius() {
        return DEFAULT_RADIUS;
    }

    @Override
    public int getMaxAllowedRadius() {
        return MAX_RADIUS;
    }

    @Override
    public boolean isExpensive() {

        return false;
    }
}
