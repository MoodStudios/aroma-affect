package com.ovrtechnology.lookup.strategy;

import com.ovrtechnology.lookup.LookupResult;
import com.ovrtechnology.lookup.LookupTarget;
import com.ovrtechnology.lookup.LookupType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.Collections;
import java.util.Set;

/**
 * Lookup strategy for finding specific blocks.
 * <p>
 * Uses an optimized spiral chunk scanning pattern that:
 * <ul>
 *   <li>Only scans already-generated chunks (no generation triggered)</li>
 *   <li>Uses spiral pattern to find nearest first</li>
 *   <li>Scans chunk sections rather than individual blocks where possible</li>
 *   <li>Short-circuits on first match</li>
 * </ul>
 * <p>
 * This is the most expensive lookup type and should always be run async.
 */
public class BlockLookupStrategy implements LookupStrategy {
    
    /**
     * Default search radius (256 blocks = 16 chunks diameter).
     */
    private static final int DEFAULT_RADIUS = 256;
    
    /**
     * Maximum search radius (1024 blocks = 64 chunks diameter).
     * Block scanning is expensive - limit to reasonable areas.
     */
    private static final int MAX_RADIUS = 1024;
    
    /**
     * Y-level scan range around origin.
     */
    private static final int Y_SCAN_RANGE = 64;
    
    @Override
    public LookupResult lookup(ServerLevel level, BlockPos origin, LookupTarget target, int maxRadius) {
        return lookup(level, origin, target, maxRadius, Collections.emptySet());
    }

    /**
     * Performs a lookup while excluding specific positions from results.
     * Used to implement blacklist: finds the nearest block that isn't excluded.
     */
    public LookupResult lookup(ServerLevel level, BlockPos origin, LookupTarget target,
                                int maxRadius, Set<BlockPos> excludedPositions) {
        if (target.type() != LookupType.BLOCK) {
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
            // Resolve the block from registry
            ResourceLocation blockId = target.resourceId();
            Block targetBlock = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);

            if (targetBlock == null) {
                return LookupResult.failure(
                        target,
                        level.dimension(),
                        origin,
                        0,
                        LookupResult.FailureReason.INVALID_TARGET
                );
            }

            int clampedRadius = Math.min(maxRadius, MAX_RADIUS);

            // Use spiral search pattern for chunk scanning
            BlockPos foundPos = spiralChunkSearch(level, origin, targetBlock, clampedRadius, excludedPositions);

            long searchTime = System.currentTimeMillis() - startTime;

            if (foundPos != null) {
                return LookupResult.success(
                        target,
                        level.dimension(),
                        origin,
                        foundPos,
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
    
    /**
     * Performs a spiral outward search through chunks to find the target block.
     * This ensures we find the nearest occurrence first.
     */
    private BlockPos spiralChunkSearch(ServerLevel level, BlockPos origin, Block targetBlock,
                                        int radius, Set<BlockPos> excludedPositions) {
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        int radiusInChunks = (radius >> 4) + 1;
        
        // Track the closest found block
        BlockPos closestFound = null;
        double closestDistanceSq = Double.MAX_VALUE;
        
        // Spiral outward from center
        for (int ring = 0; ring <= radiusInChunks; ring++) {
            // Check all chunks in this ring
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    // Only process chunks on the edge of this ring
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    
                    int chunkX = originChunkX + dx;
                    int chunkZ = originChunkZ + dz;
                    
                    // Only scan already-generated chunks
                    ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                    if (chunk == null) {
                        continue;
                    }
                    
                    // Scan this chunk for the target block
                    BlockPos found = scanChunkForBlock(level, chunk, origin, targetBlock, excludedPositions);
                    if (found != null) {
                        double distSq = origin.distSqr(found);
                        if (distSq < closestDistanceSq) {
                            closestDistanceSq = distSq;
                            closestFound = found;
                        }
                    }
                }
            }
            
            // Early exit: if we found something in this ring, and the next ring
            // is entirely farther away, we can stop
            if (closestFound != null) {
                double nextRingMinDistSq = Math.pow((ring + 1) * 16, 2);
                if (nextRingMinDistSq > closestDistanceSq) {
                    return closestFound;
                }
            }
        }
        
        return closestFound;
    }
    
    /**
     * Scans a single chunk for the target block.
     * Returns the closest occurrence to the origin within this chunk.
     */
    private BlockPos scanChunkForBlock(
            ServerLevel level,
            ChunkAccess chunk,
            BlockPos origin,
            Block targetBlock,
            Set<BlockPos> excludedPositions
    ) {
        int minY = Math.max(level.getMinY(), origin.getY() - Y_SCAN_RANGE);
        int maxY = Math.min(level.getMaxY(), origin.getY() + Y_SCAN_RANGE);
        
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        
        BlockPos closestInChunk = null;
        double closestDistSq = Double.MAX_VALUE;
        
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        
        for (int y = minY; y <= maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    mutablePos.set(chunkMinX + x, y, chunkMinZ + z);
                    
                    BlockState state = chunk.getBlockState(mutablePos);
                    if (state.is(targetBlock)
                            && (excludedPositions.isEmpty() || !excludedPositions.contains(mutablePos))) {
                        double distSq = origin.distSqr(mutablePos);
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            closestInChunk = mutablePos.immutable();
                        }
                    }
                }
            }
        }
        
        return closestInChunk;
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
        // Block scanning is always expensive
        return true;
    }
    
    @Override
    public boolean shouldCache() {
        // Blocks should NOT be cached because they can change dynamically
        // (broken by players, destroyed by explosions, moved by pistons, etc.)
        return false;
    }
}

