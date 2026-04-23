package com.ovrtechnology.lookup.worker;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public class GenericSearchWorker extends AbstractStructureSearchWorker<StructurePlacement> {

    private final int startChunkX;
    private final int startChunkZ;
    private final int maxChunkRadius;

    private int currentChunkX;
    private int currentChunkZ;
    private int length = 0;
    private double nextLength = 1;
    private Direction direction = Direction.NORTH;

    public GenericSearchWorker(
            ServerLevel level,
            BlockPos startPos,
            Structure structure,
            ResourceLocation structureId,
            StructurePlacement placement,
            int maxRadius,
            int maxSamples,
            Consumer<StructureSearchResult> callback) {
        super(level, startPos, structure, structureId, placement, maxRadius, maxSamples, callback);

        this.startChunkX = startPos.getX() >> 4;
        this.startChunkZ = startPos.getZ() >> 4;
        this.currentChunkX = startChunkX;
        this.currentChunkZ = startChunkZ;
        this.maxChunkRadius = maxRadius / 16;
    }

    @Override
    public boolean hasWork() {
        if (!super.hasWork()) {
            return false;
        }

        int distanceFromOrigin =
                Math.max(
                        Math.abs(currentChunkX - startChunkX),
                        Math.abs(currentChunkZ - startChunkZ));
        return distanceFromOrigin <= maxChunkRadius;
    }

    @Override
    public boolean doWork() {
        if (!hasWork()) {
            fail();
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(currentChunkX, currentChunkZ);
        currentPos =
                new BlockPos(
                        SectionPos.sectionToBlockCoord(chunkPos.x, 8),
                        0,
                        SectionPos.sectionToBlockCoord(chunkPos.z, 8));

        BlockPos pos = checkStructureAt(chunkPos);
        samples++;

        int currentRadius =
                Math.max(
                                Math.abs(currentChunkX - startChunkX),
                                Math.abs(currentChunkZ - startChunkZ))
                        * 16;
        logProgress(currentRadius);

        if (pos != null) {

            succeed(pos);
            return false;
        }

        switch (direction) {
            case NORTH -> currentChunkZ--;
            case EAST -> currentChunkX++;
            case SOUTH -> currentChunkZ++;
            case WEST -> currentChunkX--;
            default -> {}
        }

        length++;
        if (length >= (int) nextLength) {
            nextLength += 0.5;
            direction = direction.getClockWise();
            length = 0;
        }

        return true;
    }

    @Override
    protected String getWorkerName() {
        return "GenericSearch";
    }
}
