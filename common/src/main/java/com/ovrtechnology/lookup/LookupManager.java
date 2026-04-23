package com.ovrtechnology.lookup;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.lookup.strategy.BiomeLookupStrategy;
import com.ovrtechnology.lookup.strategy.BlockLookupStrategy;
import com.ovrtechnology.lookup.strategy.LookupStrategy;
import com.ovrtechnology.lookup.strategy.StructureLookupStrategy;
import com.ovrtechnology.lookup.worker.StructureSearchWorkerManager;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

public final class LookupManager {

    private static final LookupManager INSTANCE = new LookupManager();

    private final ExecutorService executor;

    private final Map<LookupType, LookupStrategy> strategies;

    @Getter private final LookupCache cache;

    private static final long DEFAULT_TIMEOUT_MS = 30_000L;

    private static final int CACHE_CLEANUP_INTERVAL = 20 * 60;

    private int tickCounter = 0;
    private MinecraftServer server;

    private LookupManager() {
        this.executor =
                new ThreadPoolExecutor(
                        2,
                        8,
                        60L,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(100),
                        r -> {
                            Thread t = new Thread(r, "AromaAffect-Lookup");
                            t.setDaemon(true);
                            return t;
                        },
                        new ThreadPoolExecutor.CallerRunsPolicy());

        this.strategies = new EnumMap<>(LookupType.class);
        this.strategies.put(LookupType.BIOME, new BiomeLookupStrategy());
        this.strategies.put(LookupType.STRUCTURE, new StructureLookupStrategy());
        this.strategies.put(LookupType.BLOCK, new BlockLookupStrategy());

        this.cache = new LookupCache();
    }

    public static LookupManager getInstance() {
        return INSTANCE;
    }

    public static void init() {

        LifecycleEvent.SERVER_STARTING.register(INSTANCE::onServerStarting);
        LifecycleEvent.SERVER_STOPPING.register(INSTANCE::onServerStopping);
        TickEvent.SERVER_POST.register(INSTANCE::onServerTick);

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
    }

    private void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter >= CACHE_CLEANUP_INTERVAL) {
            tickCounter = 0;
            cache.cleanup();
        }
    }

    public LookupResult lookup(ServerLevel level, BlockPos origin, LookupTarget target) {
        return lookup(level, origin, target, -1);
    }

    public LookupResult lookup(
            ServerLevel level, BlockPos origin, LookupTarget target, int radius) {

        LookupStrategy strategy = strategies.get(target.type());
        if (strategy == null) {
            return LookupResult.failure(
                    target,
                    level.dimension(),
                    origin,
                    0,
                    LookupResult.FailureReason.INVALID_TARGET);
        }

        if (strategy.shouldCache()) {
            Optional<LookupResult> cached = cache.get(target, level.dimension(), origin);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        int searchRadius = radius > 0 ? radius : strategy.getDefaultRadius();

        LookupResult result = strategy.lookup(level, origin, target, searchRadius);

        if (result.isSuccess() && strategy.shouldCache()) {
            cache.put(result);
        }

        return result;
    }

    public CompletableFuture<LookupResult> lookupAsync(
            ServerLevel level, BlockPos origin, LookupTarget target) {
        return lookupAsync(level, origin, target, -1);
    }

    public CompletableFuture<LookupResult> lookupAsync(
            ServerLevel level, BlockPos origin, LookupTarget target, int radius) {
        LookupStrategy strategy = strategies.get(target.type());
        if (strategy == null) {
            return CompletableFuture.completedFuture(
                    LookupResult.failure(
                            target,
                            level.dimension(),
                            origin,
                            0,
                            LookupResult.FailureReason.INVALID_TARGET));
        }

        if (strategy.shouldCache()) {
            Optional<LookupResult> cached = cache.get(target, level.dimension(), origin);
            if (cached.isPresent()) {
                return CompletableFuture.completedFuture(cached.get());
            }
        }

        int searchRadius = radius > 0 ? radius : strategy.getDefaultRadius();
        final boolean shouldCache = strategy.shouldCache();

        if (!strategy.isExpensive()) {
            LookupResult result = strategy.lookup(level, origin, target, searchRadius);
            if (result.isSuccess() && shouldCache) {
                cache.put(result);
            }
            return CompletableFuture.completedFuture(result);
        }

        if (target.type() == LookupType.STRUCTURE
                && strategy instanceof StructureLookupStrategy structureStrategy) {
            CompletableFuture<LookupResult> future = new CompletableFuture<>();

            structureStrategy.lookupWithCallback(
                    level,
                    origin,
                    target,
                    searchRadius,
                    result -> {
                        if (result.isSuccess() && shouldCache) {
                            cache.put(result);
                        }
                        future.complete(result);
                    });

            return future;
        }

        final MinecraftServer capturedServer = this.server;

        return CompletableFuture.supplyAsync(
                () -> {
                    if (capturedServer != null) {
                        try {
                            return capturedServer
                                    .submit(
                                            () -> {
                                                LookupResult result =
                                                        strategy.lookup(
                                                                level,
                                                                origin,
                                                                target,
                                                                searchRadius);
                                                if (result.isSuccess() && shouldCache) {
                                                    cache.put(result);
                                                }
                                                return result;
                                            })
                                    .get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException e) {
                            return LookupResult.failure(
                                    target,
                                    level.dimension(),
                                    origin,
                                    DEFAULT_TIMEOUT_MS,
                                    LookupResult.FailureReason.TIMEOUT);
                        } catch (Exception e) {
                            return LookupResult.failure(
                                    target,
                                    level.dimension(),
                                    origin,
                                    0,
                                    LookupResult.FailureReason.ERROR);
                        }
                    }
                    return LookupResult.failure(
                            target, level.dimension(), origin, 0, LookupResult.FailureReason.ERROR);
                },
                executor);
    }

    public void lookupAsync(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            Consumer<LookupResult> callback) {
        lookupAsync(level, origin, target, -1, callback);
    }

    public void lookupAsync(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            int radius,
            Consumer<LookupResult> callback) {
        lookupAsync(level, origin, target, radius)
                .thenAccept(
                        result -> {
                            if (server != null) {
                                server.execute(() -> callback.accept(result));
                            } else {
                                callback.accept(result);
                            }
                        });
    }

    public void lookupAsyncWithExclusions(
            ServerLevel level,
            BlockPos origin,
            LookupTarget target,
            int radius,
            Set<BlockPos> excludedPositions,
            Consumer<LookupResult> callback) {
        if ((target.type() != LookupType.BLOCK && target.type() != LookupType.FLOWER)
                || excludedPositions.isEmpty()) {
            lookupAsync(level, origin, target, radius, callback);
            return;
        }

        LookupStrategy strategy = strategies.get(target.type());
        if (strategy == null) {
            callback.accept(
                    LookupResult.failure(
                            target,
                            level.dimension(),
                            origin,
                            0,
                            LookupResult.FailureReason.INVALID_TARGET));
            return;
        }

        int searchRadius = radius > 0 ? radius : strategy.getDefaultRadius();
        BlockLookupStrategy blockStrategy = (BlockLookupStrategy) strategy;

        final MinecraftServer capturedServer = this.server;

        CompletableFuture.supplyAsync(
                        () -> {
                            if (capturedServer != null) {
                                try {
                                    return capturedServer
                                            .submit(
                                                    () ->
                                                            blockStrategy.lookup(
                                                                    level,
                                                                    origin,
                                                                    target,
                                                                    searchRadius,
                                                                    excludedPositions))
                                            .get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                                } catch (TimeoutException e) {
                                    return LookupResult.failure(
                                            target,
                                            level.dimension(),
                                            origin,
                                            DEFAULT_TIMEOUT_MS,
                                            LookupResult.FailureReason.TIMEOUT);
                                } catch (Exception e) {
                                    return LookupResult.failure(
                                            target,
                                            level.dimension(),
                                            origin,
                                            0,
                                            LookupResult.FailureReason.ERROR);
                                }
                            }
                            return LookupResult.failure(
                                    target,
                                    level.dimension(),
                                    origin,
                                    0,
                                    LookupResult.FailureReason.ERROR);
                        },
                        executor)
                .thenAccept(
                        result -> {
                            if (capturedServer != null) {
                                capturedServer.execute(() -> callback.accept(result));
                            } else {
                                callback.accept(result);
                            }
                        });
    }

    public int findYLevel(ServerLevel level, int x, int z, LookupType type) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
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

    public void clearCache() {
        cache.clearAll();
    }
}
