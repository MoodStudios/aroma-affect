package com.ovrtechnology.lookup.strategy;

import com.ovrtechnology.lookup.LookupResult;
import com.ovrtechnology.lookup.LookupTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Strategy interface for executing lookups.
 * Each lookup type (biome, structure, block) has its own optimized strategy.
 */
public interface LookupStrategy {
    
    /**
     * Executes a synchronous lookup.
     * <p>
     * <b>Warning:</b> This may be called off the main thread for expensive operations.
     * Implementations must be thread-safe or document their threading requirements.
     * 
     * @param level the server level to search in
     * @param origin the search origin position
     * @param target the target to find
     * @param maxRadius the maximum search radius in blocks
     * @return the lookup result
     */
    LookupResult lookup(ServerLevel level, BlockPos origin, LookupTarget target, int maxRadius);
    
    /**
     * Gets the default search radius for this strategy.
     */
    int getDefaultRadius();
    
    /**
     * Gets the maximum allowed radius for this strategy.
     * This prevents excessive resource usage.
     */
    int getMaxAllowedRadius();
    
    /**
     * Whether this strategy's lookup is expensive and should be executed async.
     */
    boolean isExpensive();
    
    /**
     * Whether results from this strategy should be cached.
     * <p>
     * Some lookup types (like blocks) should not be cached because
     * their targets can change dynamically (broken, placed, etc.).
     * Static targets like biomes and structures are safe to cache.
     * 
     * @return true if results should be cached, false otherwise
     */
    default boolean shouldCache() {
        return true;
    }
}

