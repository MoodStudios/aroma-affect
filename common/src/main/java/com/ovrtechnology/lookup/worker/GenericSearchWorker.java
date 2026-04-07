package com.ovrtechnology.lookup.worker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.function.Consumer;

/**
 * Generic spiral search worker for unknown placement types.
 * <p>
 * This is a fallback for any placement type that isn't specifically
 * handled. It searches in a spiral pattern outward from the start position.
 */
public class GenericSearchWorker extends AbstractStructureSearchWorker<StructurePlacement> {
    
    private final int startChunkX;
    private final int startChunkZ;
    private final int maxChunkRadius;
    
    // Spiral search state
    private int currentChunkX;
    private int currentChunkZ;
    private int length = 0;
    private double nextLength = 1;
    private Direction direction = Direction.NORTH;
    
    public GenericSearchWorker(
            ServerLevel level,
            BlockPos startPos,
            Structure structure,
            Identifier structureId,
            StructurePlacement placement,
            int maxRadius,
            int maxSamples,
            Consumer<StructureSearchResult> callback
    ) {
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
        
        // Check if we've exceeded the max chunk radius
        int distanceFromOrigin = Math.max(
                Math.abs(currentChunkX - startChunkX),
                Math.abs(currentChunkZ - startChunkZ)
        );
        return distanceFromOrigin <= maxChunkRadius;
    }
    
    @Override
    public boolean doWork() {
        if (!hasWork()) {
            fail();
            return false;
        }
        
        // Check current position
        ChunkPos chunkPos = new ChunkPos(currentChunkX, currentChunkZ);
        currentPos = new BlockPos(
                SectionPos.sectionToBlockCoord(chunkPos.x, 8),
                0,
                SectionPos.sectionToBlockCoord(chunkPos.z, 8)
        );
        
        BlockPos pos = checkStructureAt(chunkPos);
        samples++;
        
        // Log progress periodically
        int currentRadius = Math.max(
                Math.abs(currentChunkX - startChunkX),
                Math.abs(currentChunkZ - startChunkZ)
        ) * 16;
        logProgress(currentRadius);
        
        if (pos != null) {
            // Generic search returns first found (spiral outward = closest first)
            succeed(pos);
            return false;
        }
        
        // Move to next position in spiral
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
        
        // Continue working in this tick
        return true;
    }
    
    @Override
    protected String getWorkerName() {
        return "GenericSearch";
    }
}
