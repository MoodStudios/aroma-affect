package com.ovrtechnology.lookup.worker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.function.Consumer;

/**
 * Search worker for structures using RandomSpreadStructurePlacement.
 * <p>
 * This covers most structures: villages, temples, monuments, mansions, etc.
 * Uses the placement's spacing to efficiently search candidate positions
 * in expanding squares.
 */
public class RandomSpreadSearchWorker extends AbstractStructureSearchWorker<RandomSpreadStructurePlacement> {
    
    private final int spacing;
    private final int startSectionX;
    private final int startSectionZ;
    
    // Search state - persists between doWork() calls
    private int x = 0;
    private int z = 0;
    private int length = 0;
    private boolean completedRing = false;
    
    public RandomSpreadSearchWorker(
            ServerLevel level,
            BlockPos startPos,
            Structure structure,
            ResourceLocation structureId,
            RandomSpreadStructurePlacement placement,
            int maxRadius,
            int maxSamples,
            Consumer<StructureSearchResult> callback
    ) {
        super(level, startPos, structure, structureId, placement, maxRadius, maxSamples, callback);
        
        this.spacing = placement.spacing();
        this.startSectionX = SectionPos.blockToSectionCoord(startPos.getX());
        this.startSectionZ = SectionPos.blockToSectionCoord(startPos.getZ());
    }
    
    @Override
    public boolean hasWork() {
        if (!super.hasWork()) {
            return false;
        }
        
        // Check if we've exceeded the max radius
        int ringDistanceBlocks = length * spacing * 16;
        if (ringDistanceBlocks > maxRadius) {
            return false;  // Stop regardless of whether we found something
        }
        
        return true;
    }
    
    @Override
    public boolean doWork() {
        // Check if we should stop (exceeded radius or max samples)
        if (!hasWork()) {
            if (foundPos != null) {
                succeed(foundPos);
            } else {
                fail();
            }
            return false;
        }
        
        // Only sample positions on the perimeter of the current square
        boolean shouldSampleX = x == -length || x == length;
        boolean shouldSampleZ = z == -length || z == length;
        
        if (shouldSampleX || shouldSampleZ) {
            int sampleX = startSectionX + (spacing * x);
            int sampleZ = startSectionZ + (spacing * z);
            
            ChunkPos chunkPos = placement.getPotentialStructureChunk(level.getSeed(), sampleX, sampleZ);
            currentPos = new BlockPos(
                    SectionPos.sectionToBlockCoord(chunkPos.x, 8),
                    0,
                    SectionPos.sectionToBlockCoord(chunkPos.z, 8)
            );
            
            BlockPos pos = checkStructureAt(chunkPos);
            samples++;
            
            if (pos != null) {
                trackFoundPosition(pos);
            }
            
            // Log progress periodically
            int currentRadiusBlocks = length * spacing * 16;
            logProgress(currentRadiusBlocks);
        }
        
        // Advance to next position
        z++;
        if (z > length) {
            z = -length;
            x++;
            if (x > length) {
                // Completed this ring - if we found something, we're done
                // (can't find anything closer in outer rings)
                if (foundPos != null && length > 0) {
                    succeed(foundPos);
                    return false;
                }
                
                // Move to next ring
                length++;
                x = -length;
                z = -length;
                
                // Check if next ring exceeds radius
                int nextRingDistance = length * spacing * 16;
                if (nextRingDistance > maxRadius) {
                    fail();
                    return false;
                }
            }
        }
        
        // Continue working in this tick
        return true;
    }
    
    @Override
    protected String getWorkerName() {
        return "RandomSpreadSearch";
    }
}
