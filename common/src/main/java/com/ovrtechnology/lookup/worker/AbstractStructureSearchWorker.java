package com.ovrtechnology.lookup.worker;

import com.ovrtechnology.AromaAffect;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractStructureSearchWorker<T extends StructurePlacement>
        implements SearchWorker {

    protected final String id;
    protected final ServerLevel level;
    protected final BlockPos startPos;
    protected final Structure structure;
    protected final ResourceLocation structureId;
    protected final T placement;
    protected final int maxRadius;
    protected final int maxSamples;
    protected final Consumer<StructureSearchResult> callback;

    protected BlockPos currentPos;
    protected int samples = 0;
    protected boolean finished = false;
    protected boolean cancelled = false;
    protected long startTimeMs;

    protected BlockPos foundPos = null;
    protected double foundDistance = Double.MAX_VALUE;

    protected AbstractStructureSearchWorker(
            ServerLevel level,
            BlockPos startPos,
            Structure structure,
            ResourceLocation structureId,
            T placement,
            int maxRadius,
            int maxSamples,
            Consumer<StructureSearchResult> callback) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.level = level;
        this.startPos = startPos;
        this.structure = structure;
        this.structureId = structureId;
        this.placement = placement;
        this.maxRadius = maxRadius;
        this.maxSamples = maxSamples;
        this.callback = callback;

        this.currentPos = startPos;
        this.startTimeMs = System.currentTimeMillis();

        if (!level.getServer().getWorldData().worldGenOptions().generateStructures()) {
            this.finished = true;
        }
    }

    @Override
    public String getId() {
        return getWorkerName() + "-" + id;
    }

    private int lastLoggedProgress = 0;
    private static final int LOG_INTERVAL_SAMPLES = 1000;

    protected int notPresentCount = 0;
    protected int presentCount = 0;
    protected int chunkLoadCount = 0;

    @Override
    public boolean hasWork() {
        return !finished && !cancelled && samples < maxSamples;
    }

    @Override
    public void cancel() {
        cancelled = true;
        finished = true;
        AromaAffect.LOGGER.debug("{}: Cancelled after {} samples", getId(), samples);
    }

    protected void succeed(BlockPos pos) {
        finished = true;
        long duration = System.currentTimeMillis() - startTimeMs;
        AromaAffect.LOGGER.info(
                "{}: Found {} at {} after {} samples in {}ms",
                getId(),
                structureId,
                pos,
                samples,
                duration);
        callback.accept(StructureSearchResult.success(structureId, pos, samples, duration));
    }

    protected void fail() {
        finished = true;
        long duration = System.currentTimeMillis() - startTimeMs;
        AromaAffect.LOGGER.info(
                "{}: Not found after {} samples in {}ms (notPresent={}, present={}, chunkLoads={})",
                getId(),
                samples,
                duration,
                notPresentCount,
                presentCount,
                chunkLoadCount);
        callback.accept(StructureSearchResult.notFound(samples, duration));
    }

    protected void logProgress(int currentRadius) {
        if (samples - lastLoggedProgress >= LOG_INTERVAL_SAMPLES) {
            lastLoggedProgress = samples;
            float progress = (float) samples / maxSamples * 100;
            long elapsed = System.currentTimeMillis() - startTimeMs;
            AromaAffect.LOGGER.info(
                    "{}: {}% complete ({} samples, radius ~{} blocks, {}ms elapsed)",
                    getId(), String.format("%.1f", progress), samples, currentRadius, elapsed);
        }
    }

    protected int getRadius() {
        int dx = currentPos.getX() - startPos.getX();
        int dz = currentPos.getZ() - startPos.getZ();
        return (int) Math.sqrt(dx * dx + dz * dz);
    }

    @Nullable
    protected BlockPos checkStructureAt(ChunkPos chunkPos) {
        try {
            StructureCheckResult result =
                    level.structureManager()
                            .checkStructurePresence(chunkPos, structure, placement, false);

            if (result == StructureCheckResult.START_NOT_PRESENT) {
                notPresentCount++;
                return null;
            }

            if (result == StructureCheckResult.START_PRESENT) {
                presentCount++;
                return placement.getLocatePos(chunkPos);
            }

            chunkLoadCount++;
            ChunkAccess chunk =
                    level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);

            StructureStart structureStart =
                    level.structureManager()
                            .getStartForStructure(SectionPos.bottomOf(chunk), structure, chunk);

            if (structureStart != null && structureStart.isValid()) {
                presentCount++;
                return placement.getLocatePos(structureStart.getChunkPos());
            }

            return null;
        } catch (Exception e) {
            AromaAffect.LOGGER.debug(
                    "Error checking structure at {}: {}", chunkPos, e.getMessage());
            return null;
        }
    }

    protected boolean trackFoundPosition(BlockPos pos) {
        double distance = startPos.distSqr(pos);
        if (distance < foundDistance) {
            foundDistance = distance;
            foundPos = pos;
            return true;
        }
        return false;
    }

    protected abstract String getWorkerName();
}
