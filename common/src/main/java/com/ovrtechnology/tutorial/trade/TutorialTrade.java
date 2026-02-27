package com.ovrtechnology.tutorial.trade;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a trade that Oliver can perform with a player.
 * <p>
 * A trade exchanges one or more input items for an output item.
 * Multiple different input items can be required simultaneously.
 */
public class TutorialTrade {

    private final String id;
    private final List<InputEntry> inputs = new ArrayList<>();
    private String outputItemId;
    private int outputCount;

    // On-complete hooks (same pattern as TutorialDialogue)
    @Nullable private String onCompleteWaypointId;
    @Nullable private String onCompleteCinematicId;
    @Nullable private String onCompleteAnimationId;
    @Nullable private String onCompleteOliverAction;

    public TutorialTrade(String id) {
        this.id = id;
        this.outputItemId = "";
        this.outputCount = 0;
    }

    public TutorialTrade(String id, List<InputEntry> inputs,
                          String outputItemId, int outputCount,
                          @Nullable String onCompleteWaypointId,
                          @Nullable String onCompleteCinematicId,
                          @Nullable String onCompleteAnimationId,
                          @Nullable String onCompleteOliverAction) {
        this.id = id;
        if (inputs != null) {
            this.inputs.addAll(inputs);
        }
        this.outputItemId = outputItemId;
        this.outputCount = outputCount;
        this.onCompleteWaypointId = onCompleteWaypointId;
        this.onCompleteCinematicId = onCompleteCinematicId;
        this.onCompleteAnimationId = onCompleteAnimationId;
        this.onCompleteOliverAction = onCompleteOliverAction;
    }

    public String getId() {
        return id;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Inputs (multiple)
    // ─────────────────────────────────────────────────────────────────────────────

    public List<InputEntry> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    public void addInput(String itemId, int count) {
        // If same item already exists, replace it
        inputs.removeIf(e -> e.itemId.equals(itemId));
        inputs.add(new InputEntry(itemId, count));
    }

    public boolean removeInput(String itemId) {
        return inputs.removeIf(e -> e.itemId.equals(itemId));
    }

    public void clearInputs() {
        inputs.clear();
    }

    public boolean hasInputs() {
        return !inputs.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Output
    // ─────────────────────────────────────────────────────────────────────────────

    public String getOutputItemId() {
        return outputItemId;
    }

    public void setOutputItemId(String outputItemId) {
        this.outputItemId = outputItemId;
    }

    public int getOutputCount() {
        return outputCount;
    }

    public void setOutputCount(int outputCount) {
        this.outputCount = outputCount;
    }

    public boolean hasOutput() {
        return outputItemId != null && !outputItemId.isEmpty() && outputCount > 0;
    }

    public boolean isComplete() {
        return hasInputs() && hasOutput();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // On-Complete Hooks
    // ─────────────────────────────────────────────────────────────────────────────

    @Nullable public String getOnCompleteWaypointId() { return onCompleteWaypointId; }
    public void setOnCompleteWaypointId(@Nullable String id) { this.onCompleteWaypointId = id; }
    public boolean hasOnCompleteWaypoint() { return onCompleteWaypointId != null && !onCompleteWaypointId.isEmpty(); }

    @Nullable public String getOnCompleteCinematicId() { return onCompleteCinematicId; }
    public void setOnCompleteCinematicId(@Nullable String id) { this.onCompleteCinematicId = id; }
    public boolean hasOnCompleteCinematic() { return onCompleteCinematicId != null && !onCompleteCinematicId.isEmpty(); }

    @Nullable public String getOnCompleteAnimationId() { return onCompleteAnimationId; }
    public void setOnCompleteAnimationId(@Nullable String id) { this.onCompleteAnimationId = id; }
    public boolean hasOnCompleteAnimation() { return onCompleteAnimationId != null && !onCompleteAnimationId.isEmpty(); }

    @Nullable public String getOnCompleteOliverAction() { return onCompleteOliverAction; }
    public void setOnCompleteOliverAction(@Nullable String action) { this.onCompleteOliverAction = action; }
    public boolean hasOnCompleteOliverAction() { return onCompleteOliverAction != null && !onCompleteOliverAction.isEmpty(); }

    /**
     * Represents a single input requirement (item ID + count).
     */
    public record InputEntry(String itemId, int count) {}
}
