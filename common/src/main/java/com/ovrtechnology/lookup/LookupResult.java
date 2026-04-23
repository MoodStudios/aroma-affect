package com.ovrtechnology.lookup;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record LookupResult(
        LookupTarget target,
        ResourceKey<Level> dimension,
        BlockPos origin,
        Optional<BlockPos> foundPosition,
        double distance,
        long searchTimeMs,
        boolean fromCache,
        FailureReason failureReason) {

    public enum FailureReason {
        NONE,
        NOT_FOUND,
        TIMEOUT,
        CANCELLED,
        INVALID_TARGET,
        DIMENSION_MISMATCH,
        ERROR
    }

    public static LookupResult success(
            LookupTarget target,
            ResourceKey<Level> dimension,
            BlockPos origin,
            BlockPos foundPosition,
            long searchTimeMs,
            boolean fromCache) {
        double distance = Math.sqrt(origin.distSqr(foundPosition));
        return new LookupResult(
                target,
                dimension,
                origin,
                Optional.of(foundPosition),
                distance,
                searchTimeMs,
                fromCache,
                FailureReason.NONE);
    }

    public static LookupResult failure(
            LookupTarget target,
            ResourceKey<Level> dimension,
            BlockPos origin,
            long searchTimeMs,
            FailureReason reason) {
        return new LookupResult(
                target, dimension, origin, Optional.empty(), -1, searchTimeMs, false, reason);
    }

    public static LookupResult notFound(
            LookupTarget target, ResourceKey<Level> dimension, BlockPos origin, long searchTimeMs) {
        return failure(target, dimension, origin, searchTimeMs, FailureReason.NOT_FOUND);
    }

    public boolean isSuccess() {
        return foundPosition.isPresent() && failureReason == FailureReason.NONE;
    }

    public BlockPos getPosition() {
        return foundPosition.orElseThrow(
                () -> new IllegalStateException("No position found for " + target));
    }

    public String getFormattedDistance() {
        if (distance < 0) return "N/A";
        return String.format("%.1f", distance);
    }
}
