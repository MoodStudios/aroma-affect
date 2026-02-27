package com.ovrtechnology.tutorial.waypoint;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a tutorial waypoint with multiple ordered positions.
 * <p>
 * Waypoints are used to guide players along a path during the tutorial.
 * Positions are numbered (1, 2, 3, ...) where:
 * <ul>
 *   <li>Position 1 is the starting point</li>
 *   <li>The highest number is the destination</li>
 *   <li>Intermediate positions create a curved/complex path</li>
 * </ul>
 */
public class TutorialWaypoint {

    private final String id;
    private final TreeMap<Integer, BlockPos> positions = new TreeMap<>();

    // Area detection (cuboid defined by two corners)
    @Nullable
    private BlockPos areaCorner1;
    @Nullable
    private BlockPos areaCorner2;

    // Chain to next waypoint
    @Nullable
    private String nextWaypointId;

    // Oliver action on completion (e.g., "follow", "stop", "walkto:x,y,z", "dialogue:id")
    @Nullable
    private String oliverAction;

    // Nose-conditional chains (noseId -> nextWaypointId)
    private final Map<String, String> noseChains = new HashMap<>();

    // Default next waypoint when no nose chain matches
    @Nullable
    private String defaultNextWaypointId;

    // Cinematic to activate on completion
    @Nullable
    private String activateCinematicId;

    public TutorialWaypoint(String id) {
        this.id = id;
    }

    public TutorialWaypoint(String id, List<BlockPos> positionList) {
        this.id = id;
        for (int i = 0; i < positionList.size(); i++) {
            if (positionList.get(i) != null) {
                positions.put(i + 1, positionList.get(i));
            }
        }
    }

    public TutorialWaypoint(String id, List<BlockPos> positionList,
                           @Nullable BlockPos areaCorner1, @Nullable BlockPos areaCorner2) {
        this(id, positionList);
        this.areaCorner1 = areaCorner1;
        this.areaCorner2 = areaCorner2;
    }

    public TutorialWaypoint(String id, List<BlockPos> positionList,
                           @Nullable BlockPos areaCorner1, @Nullable BlockPos areaCorner2,
                           @Nullable String nextWaypointId) {
        this(id, positionList, areaCorner1, areaCorner2);
        this.nextWaypointId = nextWaypointId;
    }

    public TutorialWaypoint(String id, List<BlockPos> positionList,
                           @Nullable BlockPos areaCorner1, @Nullable BlockPos areaCorner2,
                           @Nullable String nextWaypointId, @Nullable String oliverAction) {
        this(id, positionList, areaCorner1, areaCorner2, nextWaypointId);
        this.oliverAction = oliverAction;
    }

    public TutorialWaypoint(String id, List<BlockPos> positionList,
                           @Nullable BlockPos areaCorner1, @Nullable BlockPos areaCorner2,
                           @Nullable String nextWaypointId, @Nullable String oliverAction,
                           @Nullable Map<String, String> noseChains, @Nullable String defaultNextWaypointId,
                           @Nullable String activateCinematicId) {
        this(id, positionList, areaCorner1, areaCorner2, nextWaypointId, oliverAction);
        if (noseChains != null) {
            this.noseChains.putAll(noseChains);
        }
        this.defaultNextWaypointId = defaultNextWaypointId;
        this.activateCinematicId = activateCinematicId;
    }

    public String getId() {
        return id;
    }

    /**
     * Sets a position at the given index (1-based).
     *
     * @param index the position index (1, 2, 3, ...)
     * @param pos   the block position
     */
    public void setPosition(int index, BlockPos pos) {
        if (index < 1) {
            throw new IllegalArgumentException("Position index must be >= 1");
        }
        if (pos == null) {
            positions.remove(index);
        } else {
            positions.put(index, pos);
        }
    }

    /**
     * Gets a position at the given index.
     *
     * @param index the position index (1-based)
     * @return the position, or null if not set
     */
    @Nullable
    public BlockPos getPosition(int index) {
        return positions.get(index);
    }

    /**
     * Removes a position at the given index.
     *
     * @param index the position index (1-based)
     * @return true if removed, false if not found
     */
    public boolean removePosition(int index) {
        return positions.remove(index) != null;
    }

    /**
     * Gets all positions in order.
     *
     * @return unmodifiable list of positions (may contain gaps as nulls)
     */
    public List<BlockPos> getPositionsOrdered() {
        if (positions.isEmpty()) {
            return Collections.emptyList();
        }
        int maxIndex = positions.lastKey();
        List<BlockPos> result = new ArrayList<>(maxIndex);
        for (int i = 1; i <= maxIndex; i++) {
            result.add(positions.get(i));
        }
        return result;
    }

    /**
     * Gets all non-null positions in order (for rendering).
     *
     * @return list of actual positions without gaps
     */
    public List<BlockPos> getValidPositions() {
        return new ArrayList<>(positions.values());
    }

    /**
     * Gets the number of defined positions.
     *
     * @return count of positions
     */
    public int getPositionCount() {
        return positions.size();
    }

    /**
     * Gets the highest position index.
     *
     * @return the max index, or 0 if no positions
     */
    public int getMaxIndex() {
        return positions.isEmpty() ? 0 : positions.lastKey();
    }

    /**
     * Gets all defined position indices.
     *
     * @return set of indices
     */
    public java.util.Set<Integer> getDefinedIndices() {
        return Collections.unmodifiableSet(positions.keySet());
    }

    /**
     * Checks if this waypoint has at least 2 positions (minimum for a path).
     *
     * @return true if the waypoint can form a valid path
     */
    public boolean isComplete() {
        return positions.size() >= 2;
    }

    /**
     * Gets the starting position (position 1 or lowest defined).
     *
     * @return the start position, or null if no positions
     */
    @Nullable
    public BlockPos getStart() {
        return positions.isEmpty() ? null : positions.firstEntry().getValue();
    }

    /**
     * Gets the ending position (highest numbered position).
     *
     * @return the end position, or null if no positions
     */
    @Nullable
    public BlockPos getEnd() {
        return positions.isEmpty() ? null : positions.lastEntry().getValue();
    }

    /**
     * Calculates the total path distance through all positions.
     *
     * @return the total distance, or -1 if incomplete
     */
    public double getTotalDistance() {
        if (!isComplete()) {
            return -1;
        }

        List<BlockPos> validPositions = getValidPositions();
        double total = 0;
        for (int i = 0; i < validPositions.size() - 1; i++) {
            total += Math.sqrt(validPositions.get(i).distSqr(validPositions.get(i + 1)));
        }
        return total;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Area Detection
    // ─────────────────────────────────────────────────────────────────────────────

    @Nullable
    public BlockPos getAreaCorner1() {
        return areaCorner1;
    }

    public void setAreaCorner1(@Nullable BlockPos pos) {
        this.areaCorner1 = pos;
    }

    @Nullable
    public BlockPos getAreaCorner2() {
        return areaCorner2;
    }

    public void setAreaCorner2(@Nullable BlockPos pos) {
        this.areaCorner2 = pos;
    }

    /**
     * Checks if the area is fully defined (both corners set).
     */
    public boolean hasArea() {
        return areaCorner1 != null && areaCorner2 != null;
    }

    /**
     * Checks if a position is inside the defined area (cuboid).
     *
     * @param pos the position to check
     * @return true if inside the area, false otherwise
     */
    public boolean isInsideArea(BlockPos pos) {
        if (!hasArea()) {
            return false;
        }

        int minX = Math.min(areaCorner1.getX(), areaCorner2.getX());
        int maxX = Math.max(areaCorner1.getX(), areaCorner2.getX());
        int minY = Math.min(areaCorner1.getY(), areaCorner2.getY());
        int maxY = Math.max(areaCorner1.getY(), areaCorner2.getY());
        int minZ = Math.min(areaCorner1.getZ(), areaCorner2.getZ());
        int maxZ = Math.max(areaCorner1.getZ(), areaCorner2.getZ());

        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    /**
     * Clears the area definition.
     */
    public void clearArea() {
        this.areaCorner1 = null;
        this.areaCorner2 = null;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Waypoint Chaining
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Gets the ID of the next waypoint in the chain.
     *
     * @return the next waypoint ID, or null if not chained
     */
    @Nullable
    public String getNextWaypointId() {
        return nextWaypointId;
    }

    /**
     * Sets the next waypoint in the chain.
     *
     * @param nextWaypointId the ID of the next waypoint, or null to unchain
     */
    public void setNextWaypointId(@Nullable String nextWaypointId) {
        this.nextWaypointId = nextWaypointId;
    }

    /**
     * Checks if this waypoint is chained to another.
     *
     * @return true if chained to a next waypoint
     */
    public boolean hasNextWaypoint() {
        return nextWaypointId != null && !nextWaypointId.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Oliver Actions
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Gets the Oliver action to execute when this waypoint is completed.
     * <p>
     * Possible values:
     * <ul>
     *   <li>"follow" - Oliver follows the player</li>
     *   <li>"stop" - Oliver becomes stationary</li>
     *   <li>"walkto:x,y,z" - Oliver walks to position</li>
     *   <li>"dialogue:id" - Set Oliver's dialogue</li>
     * </ul>
     *
     * @return the action string, or null if no action
     */
    @Nullable
    public String getOliverAction() {
        return oliverAction;
    }

    /**
     * Sets the Oliver action to execute when this waypoint is completed.
     *
     * @param action the action string, or null to clear
     */
    public void setOliverAction(@Nullable String action) {
        this.oliverAction = action;
    }

    /**
     * Checks if this waypoint has an Oliver action.
     */
    public boolean hasOliverAction() {
        return oliverAction != null && !oliverAction.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Nose Chain Conditions
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Adds a nose chain condition.
     * <p>
     * When a player completes this waypoint, if they have the specified nose equipped,
     * the specified next waypoint will be activated instead of the default chain.
     *
     * @param noseId         the nose ID to match
     * @param nextWaypointId the waypoint ID to activate
     */
    public void addNoseChain(String noseId, String nextWaypointId) {
        if (noseId != null && !noseId.isEmpty() && nextWaypointId != null && !nextWaypointId.isEmpty()) {
            noseChains.put(noseId, nextWaypointId);
        }
    }

    /**
     * Removes a nose chain condition.
     *
     * @param noseId the nose ID to remove
     * @return true if removed, false if not found
     */
    public boolean removeNoseChain(String noseId) {
        return noseChains.remove(noseId) != null;
    }

    /**
     * Gets the nose chain conditions.
     *
     * @return unmodifiable map of nose ID to next waypoint ID
     */
    public Map<String, String> getNoseChains() {
        return Collections.unmodifiableMap(noseChains);
    }

    /**
     * Checks if this waypoint has any nose chain conditions.
     */
    public boolean hasNoseChains() {
        return !noseChains.isEmpty();
    }

    /**
     * Gets the next waypoint ID for a specific nose.
     * <p>
     * Priority order:
     * <ol>
     *   <li>Nose chain match (if player has a matching nose)</li>
     *   <li>Default next waypoint ID (if set)</li>
     *   <li>Regular next waypoint ID (legacy support)</li>
     * </ol>
     *
     * @param noseId the nose ID to check (can be null if no nose equipped)
     * @return the next waypoint ID, or null if no chain
     */
    @Nullable
    public String getNextWaypointForNose(@Nullable String noseId) {
        // Check nose chain first
        if (noseId != null && noseChains.containsKey(noseId)) {
            return noseChains.get(noseId);
        }

        // Fall back to default next waypoint
        if (defaultNextWaypointId != null && !defaultNextWaypointId.isEmpty()) {
            return defaultNextWaypointId;
        }

        // Fall back to regular next waypoint (legacy)
        return nextWaypointId;
    }

    /**
     * Gets the default next waypoint ID (used when no nose chain matches).
     *
     * @return the default next waypoint ID, or null if not set
     */
    @Nullable
    public String getDefaultNextWaypointId() {
        return defaultNextWaypointId;
    }

    /**
     * Sets the default next waypoint ID.
     *
     * @param defaultNextWaypointId the default next waypoint ID, or null to clear
     */
    public void setDefaultNextWaypointId(@Nullable String defaultNextWaypointId) {
        this.defaultNextWaypointId = defaultNextWaypointId;
    }

    /**
     * Checks if this waypoint has a default next waypoint.
     */
    public boolean hasDefaultNextWaypoint() {
        return defaultNextWaypointId != null && !defaultNextWaypointId.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Cinematic Integration
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Gets the cinematic ID to activate when this waypoint is completed.
     *
     * @return the cinematic ID, or null if none
     */
    @Nullable
    public String getActivateCinematicId() {
        return activateCinematicId;
    }

    /**
     * Sets the cinematic ID to activate when this waypoint is completed.
     *
     * @param cinematicId the cinematic ID, or null to clear
     */
    public void setActivateCinematicId(@Nullable String cinematicId) {
        this.activateCinematicId = cinematicId;
    }

    /**
     * Checks if this waypoint has a cinematic to activate.
     */
    public boolean hasActivateCinematic() {
        return activateCinematicId != null && !activateCinematicId.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Legacy compatibility methods
    // ─────────────────────────────────────────────────────────────────────────────

    @Deprecated
    @Nullable
    public BlockPos getPosA() {
        return getStart();
    }

    @Deprecated
    public void setPosA(@Nullable BlockPos pos) {
        setPosition(1, pos);
    }

    @Deprecated
    @Nullable
    public BlockPos getPosB() {
        return getEnd();
    }

    @Deprecated
    public void setPosB(@Nullable BlockPos pos) {
        setPosition(2, pos);
    }

    @Deprecated
    public double getDistance() {
        return getTotalDistance();
    }
}
