package com.ovrtechnology.nose.client;

import com.ovrtechnology.AromaAffect;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NoseRenderPreferencesManager {

    public record NosePrefs(boolean noseEnabled, boolean strapEnabled) {
        public static final NosePrefs DEFAULT = new NosePrefs(true, false);
    }

    private static final Map<UUID, NosePrefs> clientCache = new ConcurrentHashMap<>();
    private static final Map<UUID, NosePrefs> serverStore = new ConcurrentHashMap<>();

    private NoseRenderPreferencesManager() {}

    public static NosePrefs getClientPrefsIfPresent(UUID playerUuid) {
        return clientCache.get(playerUuid);
    }

    public static void setClientPrefs(UUID playerUuid, boolean noseEnabled, boolean strapEnabled) {
        NosePrefs oldPrefs = clientCache.get(playerUuid);
        NosePrefs newPrefs = new NosePrefs(noseEnabled, strapEnabled);
        clientCache.put(playerUuid, newPrefs);
        if (oldPrefs == null || !oldPrefs.equals(newPrefs)) {
            AromaAffect.LOGGER.debug(
                    "[NOSE-CACHE] CLIENT set uuid={} nose={} strap={}",
                    shortUuid(playerUuid),
                    noseEnabled,
                    strapEnabled);
        }
    }

    public static void clearClientCache() {
        clientCache.clear();
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

    private static String shortUuid(UUID uuid) {
        if (uuid == null) return "null";
        String s = uuid.toString();
        return s.substring(0, 8) + "...";
    }
}
