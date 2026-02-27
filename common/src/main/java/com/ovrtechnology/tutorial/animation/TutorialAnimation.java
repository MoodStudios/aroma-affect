package com.ovrtechnology.tutorial.animation;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a tutorial animation that can dramatically remove blocks in a cuboid region.
 * <p>
 * Animations define a cuboid area (corner1 to corner2) and a type that determines
 * the visual style (explosion, door opening, debris clearing). When played, blocks
 * within the cuboid are progressively set to AIR with particles and sounds.
 */
public class TutorialAnimation {

    private final String id;
    private TutorialAnimationType type;
    @Nullable
    private BlockPos corner1;
    @Nullable
    private BlockPos corner2;
    private boolean played;
    private Map<BlockPos, String> savedBlocks = new HashMap<>();

    // What to do when the animation completes
    @Nullable
    private String onCompleteWaypointId;
    @Nullable
    private String onCompleteCinematicId;
    @Nullable
    private String onCompleteOliverAction;

    public TutorialAnimation(String id) {
        this.id = id;
        this.type = TutorialAnimationType.WALL_BREAK;
        this.played = false;
    }

    public TutorialAnimation(String id, TutorialAnimationType type,
                             @Nullable BlockPos corner1, @Nullable BlockPos corner2,
                             boolean played, Map<BlockPos, String> savedBlocks,
                             @Nullable String onCompleteWaypointId,
                             @Nullable String onCompleteCinematicId,
                             @Nullable String onCompleteOliverAction) {
        this.id = id;
        this.type = type;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.played = played;
        this.savedBlocks = savedBlocks != null ? new HashMap<>(savedBlocks) : new HashMap<>();
        this.onCompleteWaypointId = onCompleteWaypointId;
        this.onCompleteCinematicId = onCompleteCinematicId;
        this.onCompleteOliverAction = onCompleteOliverAction;
    }

    public String getId() {
        return id;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Type
    // ─────────────────────────────────────────────────────────────────────────────

    public TutorialAnimationType getType() {
        return type;
    }

    public void setType(TutorialAnimationType type) {
        this.type = type;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Corners
    // ─────────────────────────────────────────────────────────────────────────────

    @Nullable
    public BlockPos getCorner1() {
        return corner1;
    }

    public void setCorner1(@Nullable BlockPos corner1) {
        this.corner1 = corner1;
    }

    @Nullable
    public BlockPos getCorner2() {
        return corner2;
    }

    public void setCorner2(@Nullable BlockPos corner2) {
        this.corner2 = corner2;
    }

    /**
     * Checks if both corners are set, making the animation ready to play.
     */
    public boolean isComplete() {
        return corner1 != null && corner2 != null;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Played State
    // ─────────────────────────────────────────────────────────────────────────────

    public boolean isPlayed() {
        return played;
    }

    public void setPlayed(boolean played) {
        this.played = played;
    }

    public void markPlayed() {
        this.played = true;
    }

    public void reset() {
        this.played = false;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Saved Blocks (snapshot for restoration on reset)
    // ─────────────────────────────────────────────────────────────────────────────

    public Map<BlockPos, String> getSavedBlocks() {
        return savedBlocks;
    }

    public void setSavedBlocks(Map<BlockPos, String> savedBlocks) {
        this.savedBlocks = savedBlocks != null ? new HashMap<>(savedBlocks) : new HashMap<>();
    }

    public void clearSavedBlocks() {
        this.savedBlocks.clear();
    }

    public boolean hasSavedBlocks() {
        return !savedBlocks.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // On Complete Actions
    // ─────────────────────────────────────────────────────────────────────────────

    @Nullable
    public String getOnCompleteWaypointId() {
        return onCompleteWaypointId;
    }

    public void setOnCompleteWaypointId(@Nullable String waypointId) {
        this.onCompleteWaypointId = waypointId;
    }

    public boolean hasOnCompleteWaypoint() {
        return onCompleteWaypointId != null && !onCompleteWaypointId.isEmpty();
    }

    @Nullable
    public String getOnCompleteCinematicId() {
        return onCompleteCinematicId;
    }

    public void setOnCompleteCinematicId(@Nullable String cinematicId) {
        this.onCompleteCinematicId = cinematicId;
    }

    public boolean hasOnCompleteCinematic() {
        return onCompleteCinematicId != null && !onCompleteCinematicId.isEmpty();
    }

    @Nullable
    public String getOnCompleteOliverAction() {
        return onCompleteOliverAction;
    }

    public void setOnCompleteOliverAction(@Nullable String action) {
        this.onCompleteOliverAction = action;
    }

    public boolean hasOnCompleteOliverAction() {
        return onCompleteOliverAction != null && !onCompleteOliverAction.isEmpty();
    }
}
