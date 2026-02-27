package com.ovrtechnology.tutorial.dialogue;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a custom dialogue for the tutorial system.
 * <p>
 * Dialogues are data-driven text entries that can be created/edited
 * via commands. They support on-complete hooks to chain tutorial steps.
 */
public class TutorialDialogue {

    private final String id;
    private String text;

    @Nullable
    private String onCompleteWaypointId;
    @Nullable
    private String onCompleteCinematicId;
    @Nullable
    private String onCompleteAnimationId;
    @Nullable
    private String onCompleteOliverAction;

    public TutorialDialogue(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public TutorialDialogue(String id, String text,
                             @Nullable String onCompleteWaypointId,
                             @Nullable String onCompleteCinematicId,
                             @Nullable String onCompleteAnimationId,
                             @Nullable String onCompleteOliverAction) {
        this.id = id;
        this.text = text;
        this.onCompleteWaypointId = onCompleteWaypointId;
        this.onCompleteCinematicId = onCompleteCinematicId;
        this.onCompleteAnimationId = onCompleteAnimationId;
        this.onCompleteOliverAction = onCompleteOliverAction;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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
    public String getOnCompleteAnimationId() {
        return onCompleteAnimationId;
    }

    public void setOnCompleteAnimationId(@Nullable String animationId) {
        this.onCompleteAnimationId = animationId;
    }

    public boolean hasOnCompleteAnimation() {
        return onCompleteAnimationId != null && !onCompleteAnimationId.isEmpty();
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
