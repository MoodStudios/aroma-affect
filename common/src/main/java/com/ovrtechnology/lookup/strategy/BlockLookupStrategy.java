package com.ovrtechnology.lookup.strategy;

import com.ovrtechnology.lookup.LookupResult;
import com.ovrtechnology.lookup.LookupTarget;
import com.ovrtechnology.lookup.LookupType;
import java.util.Collections;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class BlockLookupStrategy implements LookupStrategy {

    private static final int DEFAULT_RADIUS = 256;

    private static final int MAX_RADIUS = 1024;

    private static final int Y_SCAN_RANGE = 64;

    @Override
    public LookupResult lookup(
            ServerLevel level, BlockPos origin, LookupTarget target, int maxRadius) {
        return lookup(level, origin, target, maxRadius, Collections.emptySet());
    }

    public LookupResult lookup(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            int maxRadius,
            Set<BlockPos> excludedPositions) {
        if (target.type() != LookupType.BLOCK) {
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.INVALID_TARGET);
        }

        long startTime = System.currentTimeMillis();

        try {

            ResourceLocation blockId = target.resourceId();
            Block targetBlock = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);

            if (targetBlock == null) {
                return LookupResult.failure(
                        target,
                        level.dimension(),
                        origin,
                        0,
                        LookupResult.FailureReason.INVALID_TARGET);
            }

            int clampedRadius = Math.min(maxRadius, MAX_RADIUS);

            BlockPos foundPos =
                    spiralChunkSearch(level, origin, targetBlock, clampedRadius, excludedPositions);

            long searchTime = System.currentTimeMillis() - startTime;

            if (foundPos != null) {
                return LookupResult.success(
                        target, level.dimension(), origin, foundPos, searchTime, false);
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

    private BlockPos spiralChunkSearch(
            ServerLevel level,
            BlockPos origin,
            Block targetBlock,
            int radius,
            Set<BlockPos> excludedPositions) {
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        int radiusInChunks = (radius >> 4) + 1;

        BlockPos closestFound = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (int ring = 0; ring <= radiusInChunks; ring++) {

            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {

                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }

                    int chunkX = originChunkX + dx;
                    int chunkZ = originChunkZ + dz;

                    ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                    if (chunk == null) {
                        continue;
                    }

                    BlockPos found =
                            scanChunkForBlock(level, chunk, origin, targetBlock, excludedPositions);
                    if (found != null) {
                        double distSq = origin.distSqr(found);
                        if (distSq < closestDistanceSq) {
                            closestDistanceSq = distSq;
                            closestFound = found;
                        }
                    }
                }
            }

            if (closestFound != null) {
                double nextRingMinDistSq = Math.pow((ring + 1) * 16, 2);
                if (nextRingMinDistSq > closestDistanceSq) {
                    return closestFound;
                }
            }
        }

        return closestFound;
    }

    private BlockPos scanChunkForBlock(
            ServerLevel level,
            ChunkAccess chunk,
            BlockPos origin,
            Block targetBlock,
            Set<BlockPos> excludedPositions) {
        int minY = Math.max(level.getMinBuildHeight(), origin.getY() - Y_SCAN_RANGE);
        int maxY = Math.min(level.getMaxBuildHeight(), origin.getY() + Y_SCAN_RANGE);

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
                            && (excludedPositions.isEmpty()
                                    || !excludedPositions.contains(mutablePos))) {
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

        return true;
    }

    @Override
    public boolean shouldCache() {

        return false;
    }
}
