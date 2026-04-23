package com.ovrtechnology.lookup.worker;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

public class ConcentricRingsSearchWorker
        extends AbstractStructureSearchWorker<ConcentricRingsStructurePlacement> {

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
            Consumer<StructureSearchResult> callback) {
        super(level, startPos, structure, structureId, placement, maxRadius, maxSamples, callback);

        this.ringPositions =
                level.getChunkSource().getGeneratorState().getRingPositionsFor(placement);

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

        float progress = (float) currentIndex / ringPositions.size() * 100;
        if (samples % 10 == 0) {
            logProgress(getRadius());
        }

        if (!hasWork()) {
            if (foundPos != null) {
                succeed(foundPos);
            } else {
                fail();
            }
            return false;
        }

        return true;
    }

    @Override
    protected String getWorkerName() {
        return "ConcentricRingsSearch";
    }
}
