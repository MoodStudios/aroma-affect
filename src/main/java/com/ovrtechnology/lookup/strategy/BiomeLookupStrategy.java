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

/**
 * Lookup strategy for finding biomes.
 * <p>
 * Uses Minecraft's native {@code findClosestBiome3d} which samples biomes
 * at intervals rather than checking every block, making it efficient for large searches.
 */
public class BiomeLookupStrategy implements LookupStrategy {
    
    /**
     * Default search radius (6400 blocks, same as /locate biome).
     */
    private static final int DEFAULT_RADIUS = 6400;
    
    /**
     * Maximum search radius (32000 blocks).
     */
    private static final int MAX_RADIUS = 32000;
    
    /**
     * Horizontal step size for biome sampling.
     * Higher = faster but may miss small biomes.
     */
    private static final int HORIZONTAL_STEP = 32;
    
    /**
     * Vertical step size for biome sampling.
     */
    private static final int VERTICAL_STEP = 64;
    
    @Override
    public LookupResult lookup(ServerLevel level, BlockPos origin, LookupTarget target, int maxRadius) {
        if (target.type() != LookupType.BIOME) {
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.INVALID_TARGET
            );
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Get the biome registry key
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, target.resourceId());
            
            // Use Minecraft's optimized biome finding
            // findClosestBiome3d(predicate, origin, radius, horizontalStep, verticalStep)
            Pair<BlockPos, Holder<Biome>> result = level.findClosestBiome3d(
                    holder -> holder.is(biomeKey),
                    origin,
                    Math.min(maxRadius, MAX_RADIUS),
                    HORIZONTAL_STEP,
                    VERTICAL_STEP
            );
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            if (result != null) {
                return LookupResult.success(
                        target,
                        level.dimension(),
                        origin,
                        result.getFirst(),
                        searchTime,
                        false
                );
            } else {
                return LookupResult.notFound(
                        target,
                        level.dimension(),
                        origin,
                        searchTime
                );
            }
        } catch (Exception e) {
            long searchTime = System.currentTimeMillis() - startTime;
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    searchTime,
                    LookupResult.FailureReason.ERROR
            );
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
        // Biome lookup is relatively fast due to sampling
        return false;
    }
}

