package com.ovrtechnology.lookup.strategy;

import com.ovrtechnology.lookup.LookupResult;
import com.ovrtechnology.lookup.LookupTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface LookupStrategy {

    LookupResult lookup(ServerLevel level, BlockPos origin, LookupTarget target, int maxRadius);

    int getDefaultRadius();

    int getMaxAllowedRadius();

    boolean isExpensive();

    default boolean shouldCache() {
        return true;
    }
}
