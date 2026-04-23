package com.ovrtechnology.lookup.worker;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

public class RandomSpreadSearchWorker
        extends AbstractStructureSearchWorker<RandomSpreadStructurePlacement> {

    private final int spacing;
    private final int startSectionX;
    private final int startSectionZ;

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
            Consumer<StructureSearchResult> callback) {
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

        int ringDistanceBlocks = length * spacing * 16;
        if (ringDistanceBlocks > maxRadius) {
            return false;
        }

        return true;
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

        boolean shouldSampleX = x == -length || x == length;
        boolean shouldSampleZ = z == -length || z == length;

        if (shouldSampleX || shouldSampleZ) {
            int sampleX = startSectionX + (spacing * x);
            int sampleZ = startSectionZ + (spacing * z);

            ChunkPos chunkPos =
                    placement.getPotentialStructureChunk(level.getSeed(), sampleX, sampleZ);
            currentPos =
                    new BlockPos(
                            SectionPos.sectionToBlockCoord(chunkPos.x, 8),
                            0,
                            SectionPos.sectionToBlockCoord(chunkPos.z, 8));

            BlockPos pos = checkStructureAt(chunkPos);
            samples++;

            if (pos != null) {
                trackFoundPosition(pos);
            }

            int currentRadiusBlocks = length * spacing * 16;
            logProgress(currentRadiusBlocks);
        }

        z++;
        if (z > length) {
            z = -length;
            x++;
            if (x > length) {

                if (foundPos != null && length > 0) {
                    succeed(foundPos);
                    return false;
                }

                length++;
                x = -length;
                z = -length;

                int nextRingDistance = length * spacing * 16;
                if (nextRingDistance > maxRadius) {
                    fail();
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected String getWorkerName() {
        return "RandomSpreadSearch";
    }
}
