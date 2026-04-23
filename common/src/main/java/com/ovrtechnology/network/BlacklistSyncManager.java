package com.ovrtechnology.network;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

public final class BlacklistSyncManager {

    private static final BlacklistSyncManager INSTANCE = new BlacklistSyncManager();

    public record ExcludedPosition(String targetId, int x, int y, int z) {}

    private final Map<UUID, List<ExcludedPosition>> excludedPositions = new ConcurrentHashMap<>();

    private BlacklistSyncManager() {}

    public static BlacklistSyncManager getInstance() {
        return INSTANCE;
    }

    public void setExclusions(UUID playerId, List<ExcludedPosition> positions) {
        if (positions.isEmpty()) {
            excludedPositions.remove(playerId);
        } else {
            excludedPositions.put(playerId, List.copyOf(positions));
        }
    }

    public Set<BlockPos> getExcludedPositionsForTarget(UUID playerId, String targetId) {
        List<ExcludedPosition> list = excludedPositions.get(playerId);
        if (list == null) return Collections.emptySet();

        Set<BlockPos> result = new HashSet<>();
        for (ExcludedPosition ep : list) {
            if (ep.targetId.equals(targetId)) {
                result.add(new BlockPos(ep.x, ep.y, ep.z));
            }
        }
        return result;
    }

    public boolean isExcludedNearby(UUID playerId, String targetId, BlockPos pos, int threshold) {
        List<ExcludedPosition> list = excludedPositions.get(playerId);
        if (list == null) return false;
        double thresholdSq = (double) threshold * threshold;
        for (ExcludedPosition ep : list) {
            if (ep.targetId.equals(targetId)) {
                double distSq = pos.distSqr(new BlockPos(ep.x, ep.y, ep.z));
                if (distSq <= thresholdSq) return true;
            }
        }
        return false;
    }
}
