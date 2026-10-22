package com.ovrtechnology.lookup;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache for lookup results with TTL expiration and spatial awareness.
 * <p>
 * The cache is organized by dimension and uses compound keys combining
 * the lookup target with a spatial region to enable cache hits for nearby queries.
 */
public class LookupCache {
    
    /**
     * Default TTL for cache entries (5 minutes).
     */
    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L;
    
    /**
     * Spatial grid size for cache bucketing.
     * Results within the same grid cell share cache entries.
     */
    private static final int SPATIAL_GRID_SIZE = 256;
    
    /**
     * Maximum distance from cached result origin to consider a cache hit.
     */
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
    
    /**
     * Attempts to get a cached result for the given lookup.
     * <p>
     * Cache hit conditions:
     * - Same dimension
     * - Same target (type + resource ID)
     * - Within MAX_CACHE_DISTANCE of the cached search origin
     * - Entry not expired
     * 
     * @param target the lookup target
     * @param dimension the dimension to search in
     * @param origin the search origin position
     * @return cached result if available and valid
     */
    public Optional<LookupResult> get(
            LookupTarget target,
            ResourceKey<Level> dimension,
            BlockPos origin
    ) {
        Map<String, CacheEntry> cache = dimensionCaches.get(dimension);
        if (cache == null) {
            return Optional.empty();
        }
        
        String key = buildCacheKey(target, origin);
        CacheEntry entry = cache.get(key);
        
        if (entry == null) {
            return Optional.empty();
        }
        
        // Check expiration
        if (entry.isExpired()) {
            cache.remove(key);
            return Optional.empty();
        }
        
        // Check spatial proximity - the cached result should still be relevant
        double distanceFromCachedOrigin = Math.sqrt(origin.distSqr(entry.result.origin()));
        if (distanceFromCachedOrigin > MAX_CACHE_DISTANCE) {
            return Optional.empty();
        }
        
        // Return a new result with updated origin and recalculated distance
        LookupResult cachedResult = entry.result;
        if (cachedResult.isSuccess()) {
            BlockPos foundPos = cachedResult.getPosition();
            double newDistance = Math.sqrt(origin.distSqr(foundPos));
            return Optional.of(new LookupResult(
                    cachedResult.target(),
                    cachedResult.dimension(),
                    origin, // Use new origin
                    cachedResult.foundPosition(),
                    newDistance, // Recalculated distance
                    cachedResult.searchTimeMs(),
                    true, // Mark as from cache
                    cachedResult.failureReason()
            ));
        }
        
        return Optional.empty();
    }
    
    /**
     * Stores a result in the cache.
     * Only successful results are cached.
     * 
     * @param result the result to cache
     */
    public void put(LookupResult result) {
        if (!result.isSuccess()) {
            return; // Don't cache failures
        }
        
        Map<String, CacheEntry> cache = dimensionCaches.computeIfAbsent(
                result.dimension(),
                k -> new ConcurrentHashMap<>()
        );
        
        String key = buildCacheKey(result.target(), result.origin());
        cache.put(key, new CacheEntry(result, System.currentTimeMillis() + ttlMs));
    }
    
    /**
     * Clears all cache entries for a dimension.
     */
    public void clearDimension(ResourceKey<Level> dimension) {
        dimensionCaches.remove(dimension);
    }
    
    /**
     * Clears all cache entries.
     */
    public void clearAll() {
        dimensionCaches.clear();
    }
    
    /**
     * Removes expired entries from all caches.
     */
    public void cleanup() {
        for (Map<String, CacheEntry> cache : dimensionCaches.values()) {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }
    
    /**
     * Gets the total number of cached entries across all dimensions.
     */
    public int size() {
        return dimensionCaches.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
    
    /**
     * Builds a cache key that includes spatial bucketing.
     */
    private String buildCacheKey(LookupTarget target, BlockPos origin) {
        // Use spatial bucketing to group nearby searches
        int gridX = Math.floorDiv(origin.getX(), SPATIAL_GRID_SIZE);
        int gridZ = Math.floorDiv(origin.getZ(), SPATIAL_GRID_SIZE);
        return target.getCacheKey() + "@" + gridX + "," + gridZ;
    }
    
    /**
     * Internal cache entry with expiration timestamp.
     */
    private record CacheEntry(LookupResult result, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}

