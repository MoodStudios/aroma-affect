package com.ovrtechnology.lookup;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import com.ovrtechnology.lookup.strategy.BiomeLookupStrategy;
import com.ovrtechnology.lookup.strategy.BlockLookupStrategy;
import com.ovrtechnology.lookup.strategy.LookupStrategy;
import com.ovrtechnology.lookup.strategy.StructureLookupStrategy;
import com.ovrtechnology.lookup.worker.StructureSearchWorkerManager;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Central manager for the lookup system.
 * <p>
 * Provides both synchronous and asynchronous lookup capabilities with:
 * <ul>
 *   <li>Automatic caching with TTL</li>
 *   <li>Async execution for expensive operations</li>
 *   <li>Per-type lookup strategies</li>
 *   <li>Configurable timeouts</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>
 * // Async lookup (recommended)
 * LookupManager.getInstance().lookupAsync(level, origin, target)
 *     .thenAccept(result -> {
 *         if (result.isSuccess()) {
 *             // Use result.getPosition()
 *         }
 *     });
 * 
 * // Sync lookup (use sparingly, may block)
 * LookupResult result = LookupManager.getInstance().lookup(level, origin, target);
 * </pre>
 */
public final class LookupManager {
    
    private static final LookupManager INSTANCE = new LookupManager();
    
    /**
     * Executor for async lookups.
     * Uses a cached thread pool with a reasonable max to prevent resource exhaustion.
     */
    private final ExecutorService executor;
    
    /**
     * Lookup strategies by type.
     */
    private final Map<LookupType, LookupStrategy> strategies;
    
    /**
     * Result cache.
     */
    private final LookupCache cache;
    
    /**
     * Default timeout for async operations.
     */
    private static final long DEFAULT_TIMEOUT_MS = 30_000L;
    
    /**
     * Cache cleanup interval in ticks (1 minute).
     */
    private static final int CACHE_CLEANUP_INTERVAL = 20 * 60;
    
    private int tickCounter = 0;
    private MinecraftServer server;
    
    private LookupManager() {
        this.executor = new ThreadPoolExecutor(
                2, // core threads
                8, // max threads
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100), // bounded queue
                r -> {
                    Thread t = new Thread(r, "AromaAffect-Lookup");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // fallback to caller if overloaded
        );
        
        this.strategies = new EnumMap<>(LookupType.class);
        this.strategies.put(LookupType.BIOME, new BiomeLookupStrategy());
        this.strategies.put(LookupType.STRUCTURE, new StructureLookupStrategy());
        this.strategies.put(LookupType.BLOCK, new BlockLookupStrategy());
        
        this.cache = new LookupCache();
    }
    
    /**
     * Gets the singleton instance.
     */
    public static LookupManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initializes the lookup manager.
     * Should be called during mod initialization.
     */
    public static void init() {
        // Register lifecycle events
        LifecycleEvent.SERVER_STARTING.register(INSTANCE::onServerStarting);
        LifecycleEvent.SERVER_STOPPING.register(INSTANCE::onServerStopping);
        TickEvent.SERVER_POST.register(INSTANCE::onServerTick);
        
        // Initialize the structure search worker manager
        StructureSearchWorkerManager.init();
        
        AromaAffect.LOGGER.info("Lookup system initialized");
    }
    
    private void onServerStarting(MinecraftServer server) {
        this.server = server;
        this.cache.clearAll();
        AromaAffect.LOGGER.debug("Lookup manager ready for server");
    }
    
    private void onServerStopping(MinecraftServer server) {
        this.server = null;
        this.cache.clearAll();
        // Don't shut down executor - it's reusable
    }
    
    private void onServerTick(MinecraftServer server) {
        if (ReplayCompat.isReplayServer(server)) return;
        tickCounter++;
        if (tickCounter >= CACHE_CLEANUP_INTERVAL) {
            tickCounter = 0;
            cache.cleanup();
        }
    }
    
    /**
     * Performs a synchronous lookup.
     * <p>
     * <b>Warning:</b> This may block if the lookup is expensive.
     * Prefer {@link #lookupAsync} for most use cases.
     * 
     * @param level the server level to search in
     * @param origin the search origin
     * @param target the target to find
     * @return the lookup result
     */
    public LookupResult lookup(ServerLevel level, BlockPos origin, LookupTarget target) {
        return lookup(level, origin, target, -1);
    }
    
    /**
     * Performs a synchronous lookup with custom radius.
     * 
     * @param level the server level to search in
     * @param origin the search origin
     * @param target the target to find
     * @param radius the search radius (-1 for default)
     * @return the lookup result
     */
    public LookupResult lookup(ServerLevel level, BlockPos origin, LookupTarget target, int radius) {
        // Get the appropriate strategy
        LookupStrategy strategy = strategies.get(target.type());
        if (strategy == null) {
            return LookupResult.failure(
                    target, level.dimension(), origin, 0,
                    LookupResult.FailureReason.INVALID_TARGET
            );
        }
        
        // Check cache first (only if strategy allows caching)
        if (strategy.shouldCache()) {
            Optional<LookupResult> cached = cache.get(target, level.dimension(), origin);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        
        int searchRadius = radius > 0 ? radius : strategy.getDefaultRadius();
        
        // Execute the lookup
        LookupResult result = strategy.lookup(level, origin, target, searchRadius);
        
        // Cache successful results (only if strategy allows caching)
        if (result.isSuccess() && strategy.shouldCache()) {
            cache.put(result);
        }
        
        return result;
    }
    
    /**
     * Performs an asynchronous lookup.
     * <p>
     * This is the recommended method for most use cases as it won't block
     * the main thread for expensive operations.
     * 
     * @param level the server level to search in
     * @param origin the search origin
     * @param target the target to find
     * @return a future that completes with the lookup result
     */
    public CompletableFuture<LookupResult> lookupAsync(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target
    ) {
        return lookupAsync(level, origin, target, -1);
    }
    
    /**
     * Performs an asynchronous lookup with custom radius.
     * <p>
     * For structure lookups, this uses an incremental worker system that
     * distributes the search across multiple server ticks to prevent freezing.
     * 
     * @param level the server level to search in
     * @param origin the search origin
     * @param target the target to find
     * @param radius the search radius (-1 for default)
     * @return a future that completes with the lookup result
     */
    public CompletableFuture<LookupResult> lookupAsync(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            int radius
    ) {
        LookupStrategy strategy = strategies.get(target.type());
        if (strategy == null) {
            return CompletableFuture.completedFuture(
                    LookupResult.failure(
                            target, level.dimension(), origin, 0,
                            LookupResult.FailureReason.INVALID_TARGET
                    )
            );
        }
        
        // Check cache first (fast path) - only if strategy allows caching
        if (strategy.shouldCache()) {
            Optional<LookupResult> cached = cache.get(target, level.dimension(), origin);
            if (cached.isPresent()) {
                return CompletableFuture.completedFuture(cached.get());
            }
        }
        
        int searchRadius = radius > 0 ? radius : strategy.getDefaultRadius();
        final boolean shouldCache = strategy.shouldCache();
        
        // For non-expensive operations, run on main thread
        if (!strategy.isExpensive()) {
            LookupResult result = strategy.lookup(level, origin, target, searchRadius);
            if (result.isSuccess() && shouldCache) {
                cache.put(result);
            }
            return CompletableFuture.completedFuture(result);
        }
        
        // For STRUCTURE lookups, use the incremental worker system
        // This prevents game freezing during large searches
        if (target.type() == LookupType.STRUCTURE && strategy instanceof StructureLookupStrategy structureStrategy) {
            CompletableFuture<LookupResult> future = new CompletableFuture<>();
            
            structureStrategy.lookupWithCallback(level, origin, target, searchRadius, result -> {
                // Cache successful results
                if (result.isSuccess() && shouldCache) {
                    cache.put(result);
                }
                future.complete(result);
            });
            
            return future;
        }
        
        // For other expensive operations (blocks), use the thread pool approach
        final MinecraftServer capturedServer = this.server;
        
        return CompletableFuture.supplyAsync(() -> {
            // Execute on the server's main thread to ensure thread safety with world access
            if (capturedServer != null) {
                try {
                    return capturedServer.submit(() -> {
                        LookupResult result = strategy.lookup(level, origin, target, searchRadius);
                        if (result.isSuccess() && shouldCache) {
                            cache.put(result);
                        }
                        return result;
                    }).get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    return LookupResult.failure(
                            target, level.dimension(), origin,
                            DEFAULT_TIMEOUT_MS,
                            LookupResult.FailureReason.TIMEOUT
                    );
                } catch (Exception e) {
                    return LookupResult.failure(
                            target, level.dimension(), origin, 0,
                            LookupResult.FailureReason.ERROR
                    );
                }
            }
            return LookupResult.failure(
                    target, level.dimension(), origin, 0,
                    LookupResult.FailureReason.ERROR
            );
        }, executor);
    }
    
    /**
     * Performs an async lookup and calls the callback with the result.
     * The callback is executed on the main server thread.
     * 
     * @param level the server level to search in
     * @param origin the search origin
     * @param target the target to find
     * @param callback the callback to receive the result
     */
    public void lookupAsync(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            Consumer<LookupResult> callback
    ) {
        lookupAsync(level, origin, target, -1, callback);
    }
    
    /**
     * Performs an async lookup with custom radius and calls the callback with the result.
     * The callback is executed on the main server thread.
     * 
     * @param level the server level to search in
     * @param origin the search origin
     * @param target the target to find
     * @param radius the search radius (-1 for default)
     * @param callback the callback to receive the result
     */
    public void lookupAsync(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            int radius,
            Consumer<LookupResult> callback
    ) {
        lookupAsync(level, origin, target, radius)
                .thenAccept(result -> {
                    // Execute callback on main thread
                    if (server != null) {
                        server.execute(() -> callback.accept(result));
                    } else {
                        callback.accept(result);
                    }
                });
    }
    
    /**
     * Performs an async block lookup with excluded positions.
     * The excluded positions are only applied for BLOCK/FLOWER lookups.
     * For other types, this delegates to the regular lookupAsync.
     */
    public void lookupAsyncWithExclusions(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            int radius,
            Set<BlockPos> excludedPositions,
            Consumer<LookupResult> callback
    ) {
        if ((target.type() != LookupType.BLOCK && target.type() != LookupType.FLOWER)
                || excludedPositions.isEmpty()) {
            lookupAsync(level, origin, target, radius, callback);
            return;
        }

        LookupStrategy strategy = strategies.get(target.type());
        if (strategy == null) {
            callback.accept(LookupResult.failure(
                    target, level.dimension(), origin, 0,
                    LookupResult.FailureReason.INVALID_TARGET));
            return;
        }

        int searchRadius = radius > 0 ? radius : strategy.getDefaultRadius();
        BlockLookupStrategy blockStrategy = (BlockLookupStrategy) strategy;

        final MinecraftServer capturedServer = this.server;

        CompletableFuture.supplyAsync(() -> {
            if (capturedServer != null) {
                try {
                    return capturedServer.submit(() ->
                            blockStrategy.lookup(level, origin, target, searchRadius, excludedPositions)
                    ).get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    return LookupResult.failure(target, level.dimension(), origin,
                            DEFAULT_TIMEOUT_MS, LookupResult.FailureReason.TIMEOUT);
                } catch (Exception e) {
                    return LookupResult.failure(target, level.dimension(), origin,
                            0, LookupResult.FailureReason.ERROR);
                }
            }
            return LookupResult.failure(target, level.dimension(), origin,
                    0, LookupResult.FailureReason.ERROR);
        }, executor).thenAccept(result -> {
            if (capturedServer != null) {
                capturedServer.execute(() -> callback.accept(result));
            } else {
                callback.accept(result);
            }
        });
    }

    /**
     * Convenience method for looking up from a player's position.
     */
    public CompletableFuture<LookupResult> lookupFromPlayer(
            ServerPlayer player,
            LookupTarget target
    ) {
        return lookupAsync(
                (ServerLevel) player.level(),
                player.blockPosition(),
                target
        );
    }


    /**
     * Finds the appropriate Y level for structure placement at (x, z).
     * <p>
     * For STRUCTURE lookups, searches from top down for a solid block with air above.
     * Falls back to highest solid block or sea level + 1.
     * <p>
     * For other lookup types, searches from sea level up for a solid block with air above.
     * Falls back to heightmap or sea level + 1.
     */
    public int findYLevel(ServerLevel level, int x, int z, LookupType type) {
        int minY = level.getMinY();
        int maxY = level.getMaxY() - 1;
        int seaLevel = level.getSeaLevel();

        if (type == LookupType.STRUCTURE) {
            for (int y = maxY; y >= minY; y--) {
                BlockPos checkPos = new BlockPos(x, y, z);
                BlockPos abovePos = new BlockPos(x, y + 1, z);

                boolean currentSolid = !level.getBlockState(checkPos).isAir();
                boolean aboveAir = (y + 1 <= maxY) && level.getBlockState(abovePos).isAir();

                if (currentSolid && aboveAir) {
                    return y + 1;
                }
            }

            for (int y = maxY; y >= minY; y--) {
                BlockPos checkPos = new BlockPos(x, y, z);
                if (!level.getBlockState(checkPos).isAir()) {
                    return Math.min(y + 1, maxY);
                }
            }

            return Math.max(minY, seaLevel + 1);

        } else {
            for (int y = seaLevel; y < maxY; y++) {
                BlockPos checkPos = new BlockPos(x, y, z);
                BlockPos abovePos = new BlockPos(x, y + 1, z);

                boolean currentSolid = !level.getBlockState(checkPos).isAir();
                boolean aboveAir = level.getBlockState(abovePos).isAir();

                if (currentSolid && aboveAir) {
                    return y + 1;
                }
            }

            try {
                int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (height > minY) return height;
            } catch (Exception ignored) {
            }

            return seaLevel + 1;
        }
    }
    
    /**
     * Gets the strategy for a lookup type.
     */
    public LookupStrategy getStrategy(LookupType type) {
        return strategies.get(type);
    }
    
    /**
     * Gets the cache.
     */
    public LookupCache getCache() {
        return cache;
    }
    
    /**
     * Clears the cache.
     */
    public void clearCache() {
        cache.clearAll();
    }
}

