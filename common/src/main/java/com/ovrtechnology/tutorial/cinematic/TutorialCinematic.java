package com.ovrtechnology.tutorial.cinematic;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a cinematic sequence for the tutorial system.
 * <p>
 * Cinematics are sequences of frames that display titles, subtitles,
 * play sounds, and trigger Oliver actions. They can block player movement
 * and are used for dramatic moments in the tutorial.
 */
public class TutorialCinematic {

    private final String id;
    private final List<CinematicFrame> frames = new ArrayList<>();

    // What to do when the cinematic completes
    @Nullable
    private String onCompleteWaypointId;
    @Nullable
    private String onCompleteOliverAction;
    @Nullable
    private String onCompleteAnimationId;

    // Nose-conditional cinematic overrides (noseId -> alternative cinematicId)
    private final Map<String, String> noseCinematicOverrides = new HashMap<>();

    public TutorialCinematic(String id) {
        this.id = id;
    }

    public TutorialCinematic(String id, List<CinematicFrame> frames,
                             @Nullable String onCompleteWaypointId,
                             @Nullable String onCompleteOliverAction,
                             @Nullable String onCompleteAnimationId,
                             @Nullable Map<String, String> noseCinematicOverrides) {
        this.id = id;
        if (frames != null) {
            this.frames.addAll(frames);
        }
        this.onCompleteWaypointId = onCompleteWaypointId;
        this.onCompleteOliverAction = onCompleteOliverAction;
        this.onCompleteAnimationId = onCompleteAnimationId;
        if (noseCinematicOverrides != null) {
            this.noseCinematicOverrides.putAll(noseCinematicOverrides);
        }
    }

    public String getId() {
        return id;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Frame Management
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Adds a new frame to the cinematic.
     *
     * @param frame the frame to add
     */
    public void addFrame(CinematicFrame frame) {
        frames.add(frame);
    }

    /**
     * Gets a frame by index (0-based).
     *
     * @param index the frame index
     * @return the frame, or null if out of bounds
     */
    @Nullable
    public CinematicFrame getFrame(int index) {
        if (index < 0 || index >= frames.size()) {
            return null;
        }
        return frames.get(index);
    }

    /**
     * Sets a frame at the specified index.
     *
     * @param index the frame index (0-based)
     * @param frame the new frame
     * @return true if set, false if index out of bounds
     */
    public boolean setFrame(int index, CinematicFrame frame) {
        if (index < 0 || index >= frames.size()) {
            return false;
        }
        frames.set(index, frame);
        return true;
    }

    /**
     * Removes a frame at the specified index.
     *
     * @param index the frame index (0-based)
     * @return true if removed, false if index out of bounds
     */
    public boolean removeFrame(int index) {
        if (index < 0 || index >= frames.size()) {
            return false;
        }
        frames.remove(index);
        return true;
    }

    /**
     * Gets all frames in the cinematic.
     *
     * @return unmodifiable list of frames
     */
    public List<CinematicFrame> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    /**
     * Gets the number of frames.
     */
    public int getFrameCount() {
        return frames.size();
    }

    /**
     * Checks if this cinematic has any frames.
     */
    public boolean hasFrames() {
        return !frames.isEmpty();
    }

    /**
     * Gets the total duration of the cinematic in ticks.
     */
    public int getTotalDuration() {
        int total = 0;
        for (CinematicFrame frame : frames) {
            total += frame.getTotalTime();
        }
        return total;
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
    public String getOnCompleteOliverAction() {
        return onCompleteOliverAction;
    }

    public void setOnCompleteOliverAction(@Nullable String action) {
        this.onCompleteOliverAction = action;
    }

    public boolean hasOnCompleteOliverAction() {
        return onCompleteOliverAction != null && !onCompleteOliverAction.isEmpty();
    }

    @Nullable
    public String getOnCompleteAnimationId() {
        return onCompleteAnimationId;
    }

    public void setOnCompleteAnimationId(@Nullable String animationId) {
        this.onCompleteAnimationId = animationId;
    }

    public boolean hasOnCompleteAnimation() {
        return onCompleteAnimationId != null && !onCompleteAnimationId.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Nose-Conditional Overrides
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Adds a nose-conditional cinematic override.
     * <p>
     * When a player with the specified nose watches this cinematic,
     * they will see the alternative cinematic instead.
     *
     * @param noseId       the nose ID to match
     * @param cinematicId  the alternative cinematic ID
     */
    public void addNoseOverride(String noseId, String cinematicId) {
        if (noseId != null && !noseId.isEmpty() && cinematicId != null && !cinematicId.isEmpty()) {
            noseCinematicOverrides.put(noseId, cinematicId);
        }
    }

    /**
     * Removes a nose-conditional cinematic override.
     *
     * @param noseId the nose ID
     * @return true if removed, false if not found
     */
    public boolean removeNoseOverride(String noseId) {
        return noseCinematicOverrides.remove(noseId) != null;
    }

    /**
     * Gets the nose-conditional cinematic overrides.
     *
     * @return unmodifiable map of nose ID to alternative cinematic ID
     */
    public Map<String, String> getNoseOverrides() {
        return Collections.unmodifiableMap(noseCinematicOverrides);
    }

    /**
     * Checks if this cinematic has any nose overrides.
     */
    public boolean hasNoseOverrides() {
        return !noseCinematicOverrides.isEmpty();
    }

    /**
     * Gets the cinematic ID to use for a player with the given nose.
     *
     * @param noseId the nose ID (can be null)
     * @return the cinematic ID to use (this cinematic's ID or an override)
     */
    public String getCinematicIdForNose(@Nullable String noseId) {
        if (noseId != null && noseCinematicOverrides.containsKey(noseId)) {
            return noseCinematicOverrides.get(noseId);
        }
        return id;
    }
}
