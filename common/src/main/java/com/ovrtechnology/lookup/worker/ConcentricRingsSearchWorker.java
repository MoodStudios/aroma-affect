package com.ovrtechnology.lookup.worker;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

import java.util.List;
import java.util.function.Consumer;

/**
 * Search worker for structures using ConcentricRingsStructurePlacement.
 * <p>
 * This is primarily used for strongholds, which have pre-calculated
 * positions in concentric rings around the world origin.
 * <p>
 * Since these positions are pre-calculated, we check ALL of them
 * and return the closest one.
 */
public class ConcentricRingsSearchWorker extends AbstractStructureSearchWorker<ConcentricRingsStructurePlacement> {
    
    private final List<ChunkPos> ringPositions;
    private int currentIndex = 0;
    
    public ConcentricRingsSearchWorker(
            ServerLevel level,
            BlockPos startPos,
            Structure structure,
            ResourceLocation structureId,
            ConcentricRingsStructurePlacement placement,
            int maxRadius,
            int maxSamples,
            Consumer<StructureSearchResult> callback
    ) {
        super(level, startPos, structure, structureId, placement, maxRadius, maxSamples, callback);
        
        this.ringPositions = level.getChunkSource()
                .getGeneratorState()
                .getRingPositionsFor(placement);
        
        if (ringPositions == null || ringPositions.isEmpty()) {
            finished = true;
        }
    }
    
    @Override
    public boolean hasWork() {
        if (!super.hasWork()) {
            return false;
        }
        return currentIndex < ringPositions.size();
    }
    
    @Override
    public boolean doWork() {
        if (!hasWork()) {
            // Finished checking all positions
            if (foundPos != null) {
                succeed(foundPos);
            } else {
                fail();
            }
            return false;
        }
        
        ChunkPos chunkPos = ringPositions.get(currentIndex);
        currentIndex++;
        samples++;
        
        BlockPos pos = checkStructureAt(chunkPos);
        if (pos != null) {
            trackFoundPosition(pos);
        }
        
        // Log progress (use index-based progress since ring positions aren't sorted by distance)
        float progress = (float) currentIndex / ringPositions.size() * 100;
        if (samples % 10 == 0) {  // Log every 10 samples for rings (usually < 128 total)
            logProgress(getRadius());
        }
        
        // Check if we're done
        if (!hasWork()) {
            if (foundPos != null) {
                succeed(foundPos);
            } else {
                fail();
            }
            return false;
        }
        
        // Continue working in this tick
        return true;
    }
    
    @Override
    protected String getWorkerName() {
        return "ConcentricRingsSearch";
    }
}
