package com.ovrtechnology.lookup;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class LookupCache {

    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L;

    private static final int SPATIAL_GRID_SIZE = 256;

    private static final double MAX_CACHE_DISTANCE = 512.0;

    private final Map<ResourceKey<Level>, Map<String, CacheEntry>> dimensionCaches;
    private final long ttlMs;

    public LookupCache() {
        this(DEFAULT_TTL_MS);
    }

    public LookupCache(long ttlMs) {
        this.dimensionCaches = new ConcurrentHashMap<>();
        this.ttlMs = ttlMs;
    }

    public Optional<LookupResult> get(
            LookupTarget target, ResourceKey<Level> dimension, BlockPos origin) {
        Map<String, CacheEntry> cache = dimensionCaches.get(dimension);
        if (cache == null) {
            return Optional.empty();
        }

        String key = buildCacheKey(target, origin);
        CacheEntry entry = cache.get(key);

        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return Optional.empty();
        }

        double distanceFromCachedOrigin = Math.sqrt(origin.distSqr(entry.result.origin()));
        if (distanceFromCachedOrigin > MAX_CACHE_DISTANCE) {
            return Optional.empty();
        }

        LookupResult cachedResult = entry.result;
        if (cachedResult.isSuccess()) {
            BlockPos foundPos = cachedResult.getPosition();
            double newDistance = Math.sqrt(origin.distSqr(foundPos));
            return Optional.of(
                    new LookupResult(
                            cachedResult.target(),
                            cachedResult.dimension(),
                            origin,
                            cachedResult.foundPosition(),
                            newDistance,
                            cachedResult.searchTimeMs(),
                            true,
                            cachedResult.failureReason()));
        }

        return Optional.empty();
    }

    public void put(LookupResult result) {
        if (!result.isSuccess()) {
            return;
        }

        Map<String, CacheEntry> cache =
                dimensionCaches.computeIfAbsent(result.dimension(), k -> new ConcurrentHashMap<>());

        String key = buildCacheKey(result.target(), result.origin());
        cache.put(key, new CacheEntry(result, System.currentTimeMillis() + ttlMs));
    }

    public void clearAll() {
        dimensionCaches.clear();
    }

    public void cleanup() {
        for (Map<String, CacheEntry> cache : dimensionCaches.values()) {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    public int size() {
        return dimensionCaches.values().stream().mapToInt(Map::size).sum();
    }

    private String buildCacheKey(LookupTarget target, BlockPos origin) {

        int gridX = Math.floorDiv(origin.getX(), SPATIAL_GRID_SIZE);
        int gridZ = Math.floorDiv(origin.getZ(), SPATIAL_GRID_SIZE);
        return target.getCacheKey() + "@" + gridX + "," + gridZ;
    }

    private record CacheEntry(LookupResult result, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
