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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Lookup strategy for finding structures.
 * <p>
 * Uses an incremental worker system inspired by Explorer's Compass to prevent
 * game freezes during expensive searches. Workers execute across multiple
 * server ticks, only using time remaining in each tick.
 * <p>
 * Supports different placement types:
 * <ul>
 *   <li>{@link RandomSpreadStructurePlacement} - Most structures (villages, temples, mansions, etc.)</li>
 *   <li>{@link ConcentricRingsStructurePlacement} - Strongholds</li>
 *   <li>Generic placements - Fallback spiral search</li>
 * </ul>
 */
public class StructureLookupStrategy implements LookupStrategy {
    
    /**
     * Default search radius in blocks (256 chunks = 4096 blocks).
     */
    private static final int DEFAULT_RADIUS = 4096;
    
    /**
     * Maximum search radius in blocks (625 chunks = 10000 blocks).
     * Matches Explorer's Compass default.
     */
    private static final int MAX_RADIUS = 10000;
    
    /**
     * Maximum samples to check before giving up.
     */
    private static final int MAX_SAMPLES = 250000;
    
    /**
     * Performs a synchronous lookup.
     * <p>
     * <b>Warning:</b> This method will START an async worker and return immediately
     * with a "searching" result. For structure lookups, prefer using
     * {@link #lookupWithCallback} instead.
     * <p>
     * This method is provided for compatibility with the LookupStrategy interface,
     * but the async version is strongly recommended for structures.
     */
    @Override
    public LookupResult lookup(ServerLevel level, BlockPos origin, LookupTarget target, int maxRadius) {
        if (target.type() != LookupType.STRUCTURE) {
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.INVALID_TARGET
            );
        }
        
        // Check if structure generation is enabled
        if (!level.getServer().getWorldData().worldGenOptions().generateStructures()) {
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.NOT_FOUND
            );
        }
        
        // For sync lookup, we use a blocking approach with a CompletableFuture
        // This is NOT recommended but maintains compatibility
        CompletableFuture<LookupResult> future = new CompletableFuture<>();
        
        boolean started = startAsyncLookup(level, origin, target, maxRadius, result -> {
            future.complete(result);
        });
        
        if (!started) {
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.INVALID_TARGET
            );
        }
        
        // Return a "searching" indicator - caller should use async method instead
        // We can't block here or we'll freeze the game
        AromaAffect.LOGGER.warn("Synchronous structure lookup called - this is not recommended. " +
                "Use lookupWithCallback() for structure searches.");
        
        // Return immediately with a "not found yet" result
        // The actual search continues in the background via workers
        return LookupResult.notFound(
                target,
                level.dimension(),
                origin,
                0
        );
    }
    
    /**
     * Starts an asynchronous structure lookup using the worker system.
     * <p>
     * This is the recommended method for structure searches. The search runs
     * incrementally across server ticks, preventing game freezes.
     * 
     * @param level the server level to search in
     * @param origin the search origin position
     * @param target the structure target to find
     * @param maxRadius the maximum search radius in blocks
     * @param callback called when the search completes (success or failure)
     * @return true if the search was started, false if invalid target
     */
    public boolean lookupWithCallback(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            int maxRadius,
            Consumer<LookupResult> callback
    ) {
        if (target.type() != LookupType.STRUCTURE) {
            callback.accept(LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.INVALID_TARGET
            ));
            return false;
        }
        
        return startAsyncLookup(level, origin, target, maxRadius, callback);
    }
    
    /**
     * Starts the async lookup by creating and registering workers.
     */
    private boolean startAsyncLookup(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            int maxRadius,
            Consumer<LookupResult> callback
    ) {
        // Check if structure generation is enabled
        if (!level.getServer().getWorldData().worldGenOptions().generateStructures()) {
            callback.accept(LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.NOT_FOUND
            ));
            return false;
        }
        
        int effectiveRadius = Math.min(maxRadius > 0 ? maxRadius : DEFAULT_RADIUS, MAX_RADIUS);
        
        try {
            // Get the structure from the registry
            Registry<Structure> structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            
            Structure structure = structureRegistry.getValue(target.resourceId());
            if (structure == null) {
                AromaAffect.LOGGER.warn("Structure not found in registry: {}", target.resourceId());
                callback.accept(LookupResult.failure(
                        target,
                        level.dimension(),
                        origin,
                        0,
                        LookupResult.FailureReason.INVALID_TARGET
                ));
                return false;
            }
            
            Holder<Structure> structureHolder = structureRegistry.wrapAsHolder(structure);
            
            // Get the placements for this structure
            List<StructurePlacement> placements = level.getChunkSource()
                    .getGeneratorState()
                    .getPlacementsForStructure(structureHolder);
            
            if (placements.isEmpty()) {
                AromaAffect.LOGGER.debug("No placements found for structure: {}", target.resourceId());
                callback.accept(LookupResult.notFound(
                        target,
                        level.dimension(),
                        origin,
                        0
                ));
                return false;
            }
            
            // Track results from multiple workers
            final AtomicReference<BlockPos> bestResult = new AtomicReference<>(null);
            final AtomicReference<Double> bestDistance = new AtomicReference<>(Double.MAX_VALUE);
            final int[] pendingWorkers = {placements.size()};
            final long startTime = System.currentTimeMillis();
            
            // Callback for each worker
            Consumer<StructureSearchResult> workerCallback = result -> {
                synchronized (pendingWorkers) {
                    pendingWorkers[0]--;
                    
                    if (result.success() && result.position() != null) {
                        double distance = origin.distSqr(result.position());
                        if (distance < bestDistance.get()) {
                            bestDistance.set(distance);
                            bestResult.set(result.position());
                        }
                    }
                    
                    // When all workers are done, call the final callback
                    if (pendingWorkers[0] <= 0) {
                        long totalTime = System.currentTimeMillis() - startTime;
                        
                        if (bestResult.get() != null) {
                            callback.accept(LookupResult.success(
                                    target,
                                    level.dimension(),
                                    origin,
                                    bestResult.get(),
                                    totalTime,
                                    false
                            ));
                        } else {
                            callback.accept(LookupResult.notFound(
                                    target,
                                    level.dimension(),
                                    origin,
                                    totalTime
                            ));
                        }
                    }
                }
            };
            
            // Create workers for each placement type
            for (StructurePlacement placement : placements) {
                createAndStartWorker(
                        level, origin, structure, target.resourceId(),
                        placement, effectiveRadius, workerCallback
                );
            }
            
            AromaAffect.LOGGER.debug("Started {} structure search workers for {}",
                    placements.size(), target.resourceId());
            
            return true;
            
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error starting structure lookup for {}: {}",
                    target.resourceId(), e.getMessage());
            callback.accept(LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.ERROR
            ));
            return false;
        }
    }
    
    /**
     * Creates and starts the appropriate worker for a placement type.
     */
    private void createAndStartWorker(
            ServerLevel level,
            BlockPos origin,
            Structure structure,
            net.minecraft.resources.Identifier structureId,
            StructurePlacement placement,
            int maxRadius,
            Consumer<StructureSearchResult> callback
    ) {
        if (placement instanceof ConcentricRingsStructurePlacement concentricPlacement) {
            AromaAffect.LOGGER.info("Starting ConcentricRings search for {} (radius={})",
                    structureId, maxRadius);
            StructureSearchWorkerManager.getInstance().addWorker(
                    new ConcentricRingsSearchWorker(
                            level, origin, structure, structureId,
                            concentricPlacement, maxRadius, MAX_SAMPLES, callback
                    )
            );
        } else if (placement instanceof RandomSpreadStructurePlacement randomPlacement) {
            AromaAffect.LOGGER.info("Starting RandomSpread search for {} (spacing={}, separation={}, radius={})",
                    structureId, randomPlacement.spacing(), randomPlacement.separation(), maxRadius);
            StructureSearchWorkerManager.getInstance().addWorker(
                    new RandomSpreadSearchWorker(
                            level, origin, structure, structureId,
                            randomPlacement, maxRadius, MAX_SAMPLES, callback
                    )
            );
        } else {
            AromaAffect.LOGGER.info("Starting Generic search for {} (radius={})",
                    structureId, maxRadius);
            StructureSearchWorkerManager.getInstance().addWorker(
                    new GenericSearchWorker(
                            level, origin, structure, structureId,
                            placement, maxRadius, MAX_SAMPLES, callback
                    )
            );
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
        // Structure lookup uses workers, but still marked as expensive
        // so LookupManager knows to handle it specially
        return true;
    }
}
