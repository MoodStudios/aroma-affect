package com.ovrtechnology.network;

import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side singleton that stores blacklisted positions per player.
 * Synced from the client via a C2S packet before each path command.
 */
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

    /**
     * Gets all excluded BlockPos entries for a specific player and target.
     */
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

    /**
     * Checks if a position is excluded for a player (exact match).
     */
    public boolean isExcluded(UUID playerId, String targetId, BlockPos pos) {
        List<ExcludedPosition> list = excludedPositions.get(playerId);
        if (list == null) return false;
        for (ExcludedPosition ep : list) {
            if (ep.targetId.equals(targetId)
                    && ep.x == pos.getX() && ep.y == pos.getY() && ep.z == pos.getZ()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a position is within a distance threshold of any excluded position
     * for the same target. Used for structures/biomes where positions may not match exactly.
     */
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

    public void removePlayer(UUID playerId) {
        excludedPositions.remove(playerId);
    }
}
