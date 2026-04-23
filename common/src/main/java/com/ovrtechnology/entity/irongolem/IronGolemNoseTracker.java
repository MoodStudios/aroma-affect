package com.ovrtechnology.entity.irongolem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IronGolemNoseTracker {

    private static final Map<UUID, Boolean> NOSE_STATE = new ConcurrentHashMap<>();

    private IronGolemNoseTracker() {}

    public static boolean hasNose(UUID golemUUID) {
        return NOSE_STATE.getOrDefault(golemUUID, true);
    }

    public static void setHasNose(UUID golemUUID, boolean hasNose) {
        NOSE_STATE.put(golemUUID, hasNose);
    }
}
