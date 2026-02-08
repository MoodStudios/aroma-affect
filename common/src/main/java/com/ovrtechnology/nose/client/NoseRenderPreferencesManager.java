package com.ovrtechnology.nose.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player nose render preferences for multiplayer sync.
 * <p>Client-side cache stores preferences received via S2C packets.
 * Server-side store holds preferences received via C2S packets.</p>
 */
public final class NoseRenderPreferencesManager {

    public record NosePrefs(boolean noseEnabled, boolean strapEnabled) {
        public static final NosePrefs DEFAULT = new NosePrefs(true, false);
    }

    private static final Map<UUID, NosePrefs> clientCache = new ConcurrentHashMap<>();
    private static final Map<UUID, NosePrefs> serverStore = new ConcurrentHashMap<>();

    private NoseRenderPreferencesManager() {
    }

    // --- Client-side methods ---

    public static NosePrefs getClientPrefs(UUID playerUuid) {
        return clientCache.getOrDefault(playerUuid, NosePrefs.DEFAULT);
    }

    /**
     * Returns client prefs only if they exist in the cache (i.e., a known remote player).
     * Returns null if the UUID is not in the cache.
     */
    public static NosePrefs getClientPrefsIfPresent(UUID playerUuid) {
        return clientCache.get(playerUuid);
    }

    public static void setClientPrefs(UUID playerUuid, boolean noseEnabled, boolean strapEnabled) {
        clientCache.put(playerUuid, new NosePrefs(noseEnabled, strapEnabled));
    }

    public static void clearClientCache() {
        clientCache.clear();
    }

    // --- Server-side methods ---

    public static NosePrefs getServerPrefs(UUID playerUuid) {
        return serverStore.getOrDefault(playerUuid, NosePrefs.DEFAULT);
    }

    public static void setServerPrefs(UUID playerUuid, boolean noseEnabled, boolean strapEnabled) {
        serverStore.put(playerUuid, new NosePrefs(noseEnabled, strapEnabled));
    }

    public static void removeServerPrefs(UUID playerUuid) {
        serverStore.remove(playerUuid);
    }

    public static Iterable<Map.Entry<UUID, NosePrefs>> getAllServerPrefs() {
        return serverStore.entrySet();
    }
}
