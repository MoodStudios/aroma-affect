package com.ovrtechnology.lookup.strategy;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.lookup.LookupResult;
import com.ovrtechnology.lookup.LookupTarget;
import com.ovrtechnology.lookup.LookupType;
import com.ovrtechnology.lookup.worker.ConcentricRingsSearchWorker;
import com.ovrtechnology.lookup.worker.GenericSearchWorker;
import com.ovrtechnology.lookup.worker.RandomSpreadSearchWorker;
import com.ovrtechnology.lookup.worker.StructureSearchResult;
import com.ovrtechnology.lookup.worker.StructureSearchWorkerManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public class StructureLookupStrategy implements LookupStrategy {

    private static final int DEFAULT_RADIUS = 4096;

    private static final int MAX_RADIUS = 10000;

    private static final int MAX_SAMPLES = 250000;

    @Override
    public LookupResult lookup(
            ServerLevel level, BlockPos origin, LookupTarget target, int maxRadius) {
        if (target.type() != LookupType.STRUCTURE) {
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.INVALID_TARGET);
        }

        if (!level.getServer().getWorldData().worldGenOptions().generateStructures()) {
            return LookupResult.failure(
                    target, level.dimension(), origin, 0, LookupResult.FailureReason.NOT_FOUND);
        }

        CompletableFuture<LookupResult> future = new CompletableFuture<>();

        boolean started =
                startAsyncLookup(
                        level,
                        origin,
                        target,
                        maxRadius,
                        result -> {
                            future.complete(result);
                        });

        if (!started) {
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.INVALID_TARGET);
        }

        AromaAffect.LOGGER.warn(
                "Synchronous structure lookup called - this is not recommended. "
                        + "Use lookupWithCallback() for structure searches.");

        return LookupResult.notFound(target, level.dimension(), origin, 0);
    }

    public boolean lookupWithCallback(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            int maxRadius,
            Consumer<LookupResult> callback) {
        if (target.type() != LookupType.STRUCTURE) {
            callback.accept(
                    LookupResult.failure(
                            target,
                            level.dimension(),
                            origin,
                            0,
                            LookupResult.FailureReason.INVALID_TARGET));
            return false;
        }

        return startAsyncLookup(level, origin, target, maxRadius, callback);
    }

    private boolean startAsyncLookup(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            int maxRadius,
            Consumer<LookupResult> callback) {

        if (!level.getServer().getWorldData().worldGenOptions().generateStructures()) {
            callback.accept(
                    LookupResult.failure(
                            target,
                            level.dimension(),
                            origin,
                            0,
                            LookupResult.FailureReason.NOT_FOUND));
            return false;
        }

        int effectiveRadius = Math.min(maxRadius > 0 ? maxRadius : DEFAULT_RADIUS, MAX_RADIUS);

        try {

            Registry<Structure> structureRegistry =
                    level.registryAccess().registryOrThrow(Registries.STRUCTURE);

            Structure structure = structureRegistry.get(target.resourceId());
            if (structure == null) {
                AromaAffect.LOGGER.warn("Structure not found in registry: {}", target.resourceId());
                callback.accept(
                        LookupResult.failure(
                                target,
                                level.dimension(),
                                origin,
                                0,
                                LookupResult.FailureReason.INVALID_TARGET));
                return false;
            }

            Holder<Structure> structureHolder = structureRegistry.wrapAsHolder(structure);

            List<StructurePlacement> placements =
                    level.getChunkSource()
                            .getGeneratorState()
                            .getPlacementsForStructure(structureHolder);

            if (placements.isEmpty()) {
                AromaAffect.LOGGER.debug(
                        "No placements found for structure: {}", target.resourceId());
                callback.accept(LookupResult.notFound(target, level.dimension(), origin, 0));
                return false;
            }

            final AtomicReference<BlockPos> bestResult = new AtomicReference<>(null);
            final AtomicReference<Double> bestDistance = new AtomicReference<>(Double.MAX_VALUE);
            final int[] pendingWorkers = {placements.size()};
            final long startTime = System.currentTimeMillis();

            Consumer<StructureSearchResult> workerCallback =
                    result -> {
                        synchronized (pendingWorkers) {
                            pendingWorkers[0]--;

                            if (result.success() && result.position() != null) {
                                double distance = origin.distSqr(result.position());
                                if (distance < bestDistance.get()) {
                                    bestDistance.set(distance);
                                    bestResult.set(result.position());
                                }
                            }

                            if (pendingWorkers[0] <= 0) {
                                long totalTime = System.currentTimeMillis() - startTime;

                                if (bestResult.get() != null) {
                                    callback.accept(
                                            LookupResult.success(
                                                    target,
                                                    level.dimension(),
                                                    origin,
                                                    bestResult.get(),
                                                    totalTime,
                                                    false));
                                } else {
                                    callback.accept(
                                            LookupResult.notFound(
                                                    target, level.dimension(), origin, totalTime));
                                }
                            }
                        }
                    };

            for (StructurePlacement placement : placements) {
                createAndStartWorker(
                        level,
                        origin,
                        structure,
                        target.resourceId(),
                        placement,
                        effectiveRadius,
                        workerCallback);
            }

            AromaAffect.LOGGER.debug(
                    "Started {} structure search workers for {}",
                    placements.size(),
                    target.resourceId());

            return true;

        } catch (Exception e) {
            AromaAffect.LOGGER.error(
                    "Error starting structure lookup for {}: {}",
                    target.resourceId(),
                    e.getMessage());
            callback.accept(
                    LookupResult.failure(
                            target,
                            level.dimension(),
                            origin,
                            0,
                            LookupResult.FailureReason.ERROR));
            return false;
        }
    }

    private void createAndStartWorker(
            ServerLevel level,
            BlockPos origin,
            Structure structure,
            net.minecraft.resources.ResourceLocation structureId,
            StructurePlacement placement,
            int maxRadius,
            Consumer<StructureSearchResult> callback) {
        if (placement instanceof ConcentricRingsStructurePlacement concentricPlacement) {
            AromaAffect.LOGGER.info(
                    "Starting ConcentricRings search for {} (radius={})", structureId, maxRadius);
            StructureSearchWorkerManager.getInstance()
                    .addWorker(
                            new ConcentricRingsSearchWorker(
                                    level,
                                    origin,
                                    structure,
                                    structureId,
                                    concentricPlacement,
                                    maxRadius,
                                    MAX_SAMPLES,
                                    callback));
        } else if (placement instanceof RandomSpreadStructurePlacement randomPlacement) {
            AromaAffect.LOGGER.info(
                    "Starting RandomSpread search for {} (spacing={}, separation={}, radius={})",
                    structureId,
                    randomPlacement.spacing(),
                    randomPlacement.separation(),
                    maxRadius);
            StructureSearchWorkerManager.getInstance()
                    .addWorker(
                            new RandomSpreadSearchWorker(
                                    level,
                                    origin,
                                    structure,
                                    structureId,
                                    randomPlacement,
                                    maxRadius,
                                    MAX_SAMPLES,
                                    callback));
        } else {
            AromaAffect.LOGGER.info(
                    "Starting Generic search for {} (radius={})", structureId, maxRadius);
            StructureSearchWorkerManager.getInstance()
                    .addWorker(
                            new GenericSearchWorker(
                                    level,
                                    origin,
                                    structure,
                                    structureId,
                                    placement,
                                    maxRadius,
                                    MAX_SAMPLES,
                                    callback));
        }
    }

    @Override
    public int getDefaultRadius() {
        return DEFAULT_RADIUS;
    }

    @Override
    public int getMaxAllowedRadius() {
        return MAX_RADIUS;
    }

    @Override
    public boolean isExpensive() {

        return true;
    }
}
