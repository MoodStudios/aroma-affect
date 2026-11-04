package com.ovrtechnology.lookup;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Represents the result of a lookup operation.
 * Contains the found position (if successful), distance, and metadata.
 */
public record LookupResult(
        LookupTarget target,
        ResourceKey<Level> dimension,
        BlockPos origin,
        Optional<BlockPos> foundPosition,
        double distance,
        long searchTimeMs,
        boolean fromCache,
        FailureReason failureReason
) {
    
    /**
     * Reasons why a lookup might fail.
     */
    public enum FailureReason {
        NONE,
        NOT_FOUND,
        TIMEOUT,
        CANCELLED,
        INVALID_TARGET,
        DIMENSION_MISMATCH,
        ERROR
    }
    
    /**
     * Creates a successful result.
     */
    public static LookupResult success(
            LookupTarget target,
            ResourceKey<Level> dimension,
            BlockPos origin,
            BlockPos foundPosition,
            long searchTimeMs,
            boolean fromCache
    ) {
        double distance = Math.sqrt(origin.distSqr(foundPosition));
        return new LookupResult(
                target, dimension, origin,
                Optional.of(foundPosition),
                distance, searchTimeMs, fromCache,
                FailureReason.NONE
        );
    }
    
    /**
     * Creates a failed result.
     */
    public static LookupResult failure(
            LookupTarget target,
            ResourceKey<Level> dimension,
            BlockPos origin,
            long searchTimeMs,
            FailureReason reason
    ) {
        return new LookupResult(
                target, dimension, origin,
                Optional.empty(),
                -1, searchTimeMs, false,
                reason
        );
    }
    
    /**
     * Creates a "not found" result.
     */
    public static LookupResult notFound(
            LookupTarget target,
            ResourceKey<Level> dimension,
            BlockPos origin,
            long searchTimeMs
    ) {
        return failure(target, dimension, origin, searchTimeMs, FailureReason.NOT_FOUND);
    }
    
    /**
     * Whether this lookup was successful.
     */
    public boolean isSuccess() {
        return foundPosition.isPresent() && failureReason == FailureReason.NONE;
    }
    
    /**
     * Gets the found position, throwing if not present.
     */
    public BlockPos getPosition() {
        return foundPosition.orElseThrow(() -> 
                new IllegalStateException("No position found for " + target));
    }
    
    /**
     * Gets a formatted distance string.
     */
    public String getFormattedDistance() {
        if (distance < 0) return "N/A";
        return String.format("%.1f", distance);
    }
    
    /**
     * Gets a formatted position string.
     */
    public String getFormattedPosition() {
        return foundPosition
                .map(pos -> String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ()))
                .orElse("Not found");
    }
}

