package com.ovrtechnology.tutorial.waypoint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ovrtechnology.AromaAffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Manages tutorial waypoints for a world.
 * <p>
 * Waypoints define multi-point paths (1, 2, 3, ...) that can be activated
 * to guide players through the tutorial with visual indicators.
 * Each waypoint can also have a detection area (cuboid) that triggers
 * deactivation when a player enters it.
 */
public class TutorialWaypointManager extends SavedData {

    private final Map<String, TutorialWaypoint> waypoints = new HashMap<>();

    private static final Codec<WaypointData> WAYPOINT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(WaypointData::id),
                    BlockPos.CODEC.listOf().fieldOf("positions").forGetter(WaypointData::positions),
                    BlockPos.CODEC.optionalFieldOf("areaCorner1").forGetter(d -> Optional.ofNullable(d.areaCorner1)),
                    BlockPos.CODEC.optionalFieldOf("areaCorner2").forGetter(d -> Optional.ofNullable(d.areaCorner2)),
                    Codec.STRING.optionalFieldOf("nextWaypointId").forGetter(d -> Optional.ofNullable(d.nextWaypointId)),
                    Codec.STRING.optionalFieldOf("oliverAction").forGetter(d -> Optional.ofNullable(d.oliverAction)),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("noseChains")
                            .forGetter(d -> d.noseChains.isEmpty() ? Optional.empty() : Optional.of(d.noseChains)),
                    Codec.STRING.optionalFieldOf("defaultNextWaypointId").forGetter(d -> Optional.ofNullable(d.defaultNextWaypointId)),
                    Codec.STRING.optionalFieldOf("activateCinematicId").forGetter(d -> Optional.ofNullable(d.activateCinematicId))
            ).apply(instance, (id, positions, c1, c2, next, oliver, noseChains, defaultNext, cinematic) ->
                    new WaypointData(id, positions, c1.orElse(null), c2.orElse(null), next.orElse(null), oliver.orElse(null),
                            noseChains.orElse(Map.of()), defaultNext.orElse(null), cinematic.orElse(null)))
    );

    private static final Codec<TutorialWaypointManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    WAYPOINT_CODEC.listOf().fieldOf("waypoints").forGetter(m -> m.getWaypointDataList())
            ).apply(instance, TutorialWaypointManager::new)
    );

    static final SavedDataType<TutorialWaypointManager> TYPE = new SavedDataType<>(
            AromaAffect.MOD_ID + "_tutorial_waypoints",
            TutorialWaypointManager::new,
            CODEC,
            null
    );

    public TutorialWaypointManager() {
    }

    private TutorialWaypointManager(List<WaypointData> waypointDataList) {
        for (WaypointData data : waypointDataList) {
            TutorialWaypoint wp = new TutorialWaypoint(
                    data.id, data.positions, data.areaCorner1, data.areaCorner2,
                    data.nextWaypointId, data.oliverAction,
                    data.noseChains, data.defaultNextWaypointId, data.activateCinematicId);
            waypoints.put(data.id, wp);
        }
    }

    private List<WaypointData> getWaypointDataList() {
        List<WaypointData> list = new ArrayList<>();
        for (TutorialWaypoint wp : waypoints.values()) {
            List<BlockPos> positions = wp.getPositionsOrdered();
            List<BlockPos> validPositions = new ArrayList<>();
            for (BlockPos pos : positions) {
                if (pos != null) {
                    validPositions.add(pos);
                }
            }
            list.add(new WaypointData(
                    wp.getId(),
                    validPositions,
                    wp.getAreaCorner1(),
                    wp.getAreaCorner2(),
                    wp.getNextWaypointId(),
                    wp.getOliverAction(),
                    new HashMap<>(wp.getNoseChains()),
                    wp.getDefaultNextWaypointId(),
                    wp.getActivateCinematicId()
            ));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Waypoint CRUD
    // ─────────────────────────────────────────────────────────────────────────────

    public static boolean createWaypoint(ServerLevel level, String id) {
        TutorialWaypointManager manager = get(level);
        if (manager.waypoints.containsKey(id)) {
            return false;
        }
        manager.waypoints.put(id, new TutorialWaypoint(id));
        manager.setDirty();
        AromaAffect.LOGGER.info("Created tutorial waypoint: {}", id);
        return true;
    }

    public static boolean deleteWaypoint(ServerLevel level, String id) {
        TutorialWaypointManager manager = get(level);
        if (manager.waypoints.remove(id) != null) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Deleted tutorial waypoint: {}", id);
            return true;
        }
        return false;
    }

    public static boolean setPosition(ServerLevel level, String id, int index, BlockPos pos) {
        TutorialWaypointManager manager = get(level);
        TutorialWaypoint waypoint = manager.waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        waypoint.setPosition(index, pos);
        manager.setDirty();
        AromaAffect.LOGGER.info("Set waypoint {} position {} to {}", id, index, pos);
        return true;
    }

    public static boolean removePosition(ServerLevel level, String id, int index) {
        TutorialWaypointManager manager = get(level);
        TutorialWaypoint waypoint = manager.waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        boolean removed = waypoint.removePosition(index);
        if (removed) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Removed waypoint {} position {}", id, index);
        }
        return removed;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Area Management
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets an area corner for a waypoint.
     *
     * @param level  the server level
     * @param id     the waypoint ID
     * @param corner which corner (1 or 2)
     * @param pos    the position
     * @return true if set, false if waypoint not found
     */
    public static boolean setAreaCorner(ServerLevel level, String id, int corner, BlockPos pos) {
        TutorialWaypointManager manager = get(level);
        TutorialWaypoint waypoint = manager.waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        if (corner == 1) {
            waypoint.setAreaCorner1(pos);
        } else if (corner == 2) {
            waypoint.setAreaCorner2(pos);
        } else {
            return false;
        }
        manager.setDirty();
        AromaAffect.LOGGER.info("Set waypoint {} area corner {} to {}", id, corner, pos);
        return true;
    }

    /**
     * Clears the area for a waypoint.
     *
     * @param level the server level
     * @param id    the waypoint ID
     * @return true if cleared, false if waypoint not found
     */
    public static boolean clearArea(ServerLevel level, String id) {
        TutorialWaypointManager manager = get(level);
        TutorialWaypoint waypoint = manager.waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        waypoint.clearArea();
        manager.setDirty();
        AromaAffect.LOGGER.info("Cleared waypoint {} area", id);
        return true;
    }

    /**
     * Gets all waypoints that have areas defined.
     *
     * @param level the server level
     * @return list of waypoints with areas
     */
    public static List<TutorialWaypoint> getWaypointsWithAreas(ServerLevel level) {
        List<TutorialWaypoint> result = new ArrayList<>();
        for (TutorialWaypoint wp : get(level).waypoints.values()) {
            if (wp.hasArea()) {
                result.add(wp);
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Queries
    // ─────────────────────────────────────────────────────────────────────────────

    public static Optional<TutorialWaypoint> getWaypoint(ServerLevel level, String id) {
        return Optional.ofNullable(get(level).waypoints.get(id));
    }

    public static Set<String> getAllWaypointIds(ServerLevel level) {
        return new HashSet<>(get(level).waypoints.keySet());
    }

    public static List<TutorialWaypoint> getCompleteWaypoints(ServerLevel level) {
        List<TutorialWaypoint> complete = new ArrayList<>();
        for (TutorialWaypoint wp : get(level).waypoints.values()) {
            if (wp.isComplete()) {
                complete.add(wp);
            }
        }
        return complete;
    }

    // Legacy compatibility
    @Deprecated
    public static boolean setPosA(ServerLevel level, String id, BlockPos pos) {
        return setPosition(level, id, 1, pos);
    }

    @Deprecated
    public static boolean setPosB(ServerLevel level, String id, BlockPos pos) {
        return setPosition(level, id, 2, pos);
    }

    private static TutorialWaypointManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Waypoint Chaining
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the next waypoint in a chain.
     *
     * @param level          the server level
     * @param id             the waypoint ID
     * @param nextWaypointId the ID of the next waypoint (null to unchain)
     * @return true if set, false if waypoint not found
     */
    public static boolean setNextWaypoint(ServerLevel level, String id, String nextWaypointId) {
        TutorialWaypointManager manager = get(level);
        TutorialWaypoint waypoint = manager.waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        waypoint.setNextWaypointId(nextWaypointId);
        manager.setDirty();
        if (nextWaypointId != null) {
            AromaAffect.LOGGER.info("Chained waypoint {} -> {}", id, nextWaypointId);
        } else {
            AromaAffect.LOGGER.info("Unchained waypoint {}", id);
        }
        return true;
    }

    /**
     * Clears the chain for a waypoint.
     *
     * @param level the server level
     * @param id    the waypoint ID
     * @return true if cleared, false if waypoint not found
     */
    public static boolean clearChain(ServerLevel level, String id) {
        return setNextWaypoint(level, id, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Oliver Actions
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the Oliver action for when this waypoint is completed.
     * <p>
     * Possible actions:
     * <ul>
     *   <li>"follow" - Oliver follows the player</li>
     *   <li>"stop" - Oliver becomes stationary</li>
     *   <li>"walkto:x,y,z" - Oliver walks to position</li>
     *   <li>"dialogue:id" - Set Oliver's dialogue</li>
     * </ul>
     *
     * @param level  the server level
     * @param id     the waypoint ID
     * @param action the action string, or null to clear
     * @return true if set, false if waypoint not found
     */
    public static boolean setOliverAction(ServerLevel level, String id, String action) {
        TutorialWaypointManager manager = get(level);
        TutorialWaypoint waypoint = manager.waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        waypoint.setOliverAction(action);
        manager.setDirty();
        if (action != null) {
            AromaAffect.LOGGER.info("Set waypoint {} oliver action: {}", id, action);
        } else {
            AromaAffect.LOGGER.info("Cleared waypoint {} oliver action", id);
        }
        return true;
    }

    /**
     * Clears the Oliver action for a waypoint.
     *
     * @param level the server level
     * @param id    the waypoint ID
     * @return true if cleared, false if waypoint not found
     */
    public static boolean clearOliverAction(ServerLevel level, String id) {
        return setOliverAction(level, id, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Nose Chain Conditions
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Adds a nose chain condition to a waypoint.
     * <p>
     * When a player completes this waypoint while wearing the specified nose,
     * the specified next waypoint will be activated.
     *
     * @param level          the server level
     * @param id             the waypoint ID
     * @param noseId         the nose ID to match
     * @param nextWaypointId the waypoint ID to activate
     * @return true if added, false if waypoint not found
     */
    public static boolean addNoseChain(ServerLevel level, String id, String noseId, String nextWaypointId) {
        TutorialWaypointManager manager = get(level);
        TutorialWaypoint waypoint = manager.waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        waypoint.addNoseChain(noseId, nextWaypointId);
        manager.setDirty();
        AromaAffect.LOGGER.info("Added nose chain to waypoint {}: {} -> {}", id, noseId, nextWaypointId);
        return true;
    }

    /**
     * Removes a nose chain condition from a waypoint.
     *
     * @param level  the server level
     * @param id     the waypoint ID
     * @param noseId the nose ID to remove
     * @return true if removed, false if waypoint or chain not found
     */
    public static boolean removeNoseChain(ServerLevel level, String id, String noseId) {
        TutorialWaypointManager manager = get(level);
        TutorialWaypoint waypoint = manager.waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        boolean removed = waypoint.removeNoseChain(noseId);
        if (removed) {
            manager.setDirty();
            AromaAffect.LOGGER.info("Removed nose chain from waypoint {}: {}", id, noseId);
        }
        return removed;
    }

    /**
     * Sets the default next waypoint ID for when no nose chain matches.
     *
     * @param level               the server level
     * @param id                  the waypoint ID
     * @param defaultNextWaypointId the default next waypoint ID (null to clear)
     * @return true if set, false if waypoint not found
     */
    public static boolean setDefaultChain(ServerLevel level, String id, String defaultNextWaypointId) {
        TutorialWaypointManager manager = get(level);
        TutorialWaypoint waypoint = manager.waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        waypoint.setDefaultNextWaypointId(defaultNextWaypointId);
        manager.setDirty();
        if (defaultNextWaypointId != null) {
            AromaAffect.LOGGER.info("Set default chain for waypoint {}: {}", id, defaultNextWaypointId);
        } else {
            AromaAffect.LOGGER.info("Cleared default chain for waypoint {}", id);
        }
        return true;
    }

    /**
     * Clears the default chain for a waypoint.
     *
     * @param level the server level
     * @param id    the waypoint ID
     * @return true if cleared, false if waypoint not found
     */
    public static boolean clearDefaultChain(ServerLevel level, String id) {
        return setDefaultChain(level, id, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Static API - Cinematic Integration
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the cinematic to activate when a waypoint is completed.
     *
     * @param level       the server level
     * @param id          the waypoint ID
     * @param cinematicId the cinematic ID to activate (null to clear)
     * @return true if set, false if waypoint not found
     */
    public static boolean setActivateCinematic(ServerLevel level, String id, String cinematicId) {
        TutorialWaypointManager manager = get(level);
        TutorialWaypoint waypoint = manager.waypoints.get(id);
        if (waypoint == null) {
            return false;
        }
        waypoint.setActivateCinematicId(cinematicId);
        manager.setDirty();
        if (cinematicId != null) {
            AromaAffect.LOGGER.info("Set cinematic for waypoint {}: {}", id, cinematicId);
        } else {
            AromaAffect.LOGGER.info("Cleared cinematic for waypoint {}", id);
        }
        return true;
    }

    /**
     * Clears the cinematic for a waypoint.
     *
     * @param level the server level
     * @param id    the waypoint ID
     * @return true if cleared, false if waypoint not found
     */
    public static boolean clearActivateCinematic(ServerLevel level, String id) {
        return setActivateCinematic(level, id, null);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal data record
    // ─────────────────────────────────────────────────────────────────────────────

    private record WaypointData(
            String id,
            List<BlockPos> positions,
            BlockPos areaCorner1,
            BlockPos areaCorner2,
            String nextWaypointId,
            String oliverAction,
            Map<String, String> noseChains,
            String defaultNextWaypointId,
            String activateCinematicId
    ) {}
}
