package com.ovrtechnology.lookup.worker;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record StructureSearchResult(
        boolean success,
        @Nullable ResourceLocation structureId,
        @Nullable BlockPos position,
        int samples,
        long searchTimeMs) {

    public static StructureSearchResult success(
            ResourceLocation structureId, BlockPos position, int samples, long searchTimeMs) {
        return new StructureSearchResult(true, structureId, position, samples, searchTimeMs);
    }

    public static StructureSearchResult notFound(int samples, long searchTimeMs) {
        return new StructureSearchResult(false, null, null, samples, searchTimeMs);
    }

    public static StructureSearchResult cancelled() {
        return new StructureSearchResult(false, null, null, 0, 0);
    }
}
