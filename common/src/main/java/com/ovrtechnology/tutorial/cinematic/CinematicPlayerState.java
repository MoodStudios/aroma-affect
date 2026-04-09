package com.ovrtechnology.tutorial.cinematic;

import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks the state of an active cinematic for a player.
 * <p>
 * This includes the current frame, remaining time, the current camera position,
 * and the original player state to restore after the cinematic completes.
 * <p>
 * Supports smooth camera transitions using linear interpolation between frames.
 */
public class CinematicPlayerState {

    private final String cinematicId;
    private int currentFrameIndex;
    private int frameTicksRemaining;
    private boolean frameActionExecuted;

    // Original player state to restore after cinematic
    private final double originalX;
    private final double originalY;
    private final double originalZ;
    private final float originalYaw;
    private final float originalPitch;
    private final GameType originalGameMode;

    // Current target camera position (from current frame)
    @Nullable private Double targetCameraX;
    @Nullable private Double targetCameraY;
    @Nullable private Double targetCameraZ;
    @Nullable private Float targetCameraYaw;
    @Nullable private Float targetCameraPitch;

    // Previous camera position (for interpolation)
    @Nullable private Double prevCameraX;
    @Nullable private Double prevCameraY;
    @Nullable private Double prevCameraZ;
    @Nullable private Float prevCameraYaw;
    @Nullable private Float prevCameraPitch;

    // Transition timing
    private int transitionTotalTicks;
    private int transitionTicksElapsed;

    // Total ticks elapsed since cinematic started
    private int totalTicksElapsed;

    /**
     * Creates a new cinematic state.
     *
     * @param cinematicId      the ID of the cinematic being played
     * @param originalX        the player's original X position
     * @param originalY        the player's original Y position
     * @param originalZ        the player's original Z position
     * @param originalYaw      the player's original yaw rotation
     * @param originalPitch    the player's original pitch rotation
     * @param originalGameMode the player's original game mode
     */
    public CinematicPlayerState(String cinematicId, double originalX, double originalY, double originalZ,
                                 float originalYaw, float originalPitch, GameType originalGameMode) {
        this.cinematicId = cinematicId;
        this.currentFrameIndex = 0;
        this.frameTicksRemaining = 0;
        this.frameActionExecuted = false;
        this.originalX = originalX;
        this.originalY = originalY;
        this.originalZ = originalZ;
        this.originalYaw = originalYaw;
        this.originalPitch = originalPitch;
        this.originalGameMode = originalGameMode;
    }

    public String getCinematicId() {
        return cinematicId;
    }

    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    public void setCurrentFrameIndex(int index) {
        this.currentFrameIndex = index;
        this.frameActionExecuted = false;
    }

    public int getFrameTicksRemaining() {
        return frameTicksRemaining;
    }

    public void setFrameTicksRemaining(int ticks) {
        this.frameTicksRemaining = ticks;
    }

    /**
     * Decrements the frame ticks remaining and advances transition.
     *
     * @return true if the frame is still active, false if it has finished
     */
    public boolean tick() {
        totalTicksElapsed++;

        // Advance transition
        if (transitionTicksElapsed < transitionTotalTicks) {
            transitionTicksElapsed++;
        }

        if (frameTicksRemaining > 0) {
            frameTicksRemaining--;
            return true;
        }
        return false;
    }

    /**
     * Gets the total ticks elapsed since this cinematic started.
     */
    public int getTotalTicksElapsed() {
        return totalTicksElapsed;
    }

    // Original position getters
    public double getOriginalX() {
        return originalX;
    }

    public double getOriginalY() {
        return originalY;
    }

    public double getOriginalZ() {
        return originalZ;
    }

    public float getOriginalYaw() {
        return originalYaw;
    }

    public float getOriginalPitch() {
        return originalPitch;
    }

    public GameType getOriginalGameMode() {
        return originalGameMode;
    }

    // Camera position methods

    /**
     * Checks if this frame has a target camera position.
     */
    public boolean hasTargetCameraPosition() {
        return targetCameraX != null && targetCameraY != null && targetCameraZ != null;
    }

    /**
     * Gets the interpolated camera X position.
     */
    public Double getInterpolatedCameraX() {
        if (targetCameraX == null) return null;
        if (prevCameraX == null || transitionTotalTicks == 0) return targetCameraX;
        return lerp(prevCameraX, targetCameraX, getTransitionProgress());
    }

    /**
     * Gets the interpolated camera Y position.
     */
    public Double getInterpolatedCameraY() {
        if (targetCameraY == null) return null;
        if (prevCameraY == null || transitionTotalTicks == 0) return targetCameraY;
        return lerp(prevCameraY, targetCameraY, getTransitionProgress());
    }

    /**
     * Gets the interpolated camera Z position.
     */
    public Double getInterpolatedCameraZ() {
        if (targetCameraZ == null) return null;
        if (prevCameraZ == null || transitionTotalTicks == 0) return targetCameraZ;
        return lerp(prevCameraZ, targetCameraZ, getTransitionProgress());
    }

    /**
     * Gets the interpolated camera yaw.
     */
    public Float getInterpolatedCameraYaw() {
        if (targetCameraYaw == null) return null;
        if (prevCameraYaw == null || transitionTotalTicks == 0) return targetCameraYaw;
        return lerpAngle(prevCameraYaw, targetCameraYaw, getTransitionProgress());
    }

    /**
     * Gets the interpolated camera pitch.
     */
    public Float getInterpolatedCameraPitch() {
        if (targetCameraPitch == null) return null;
        if (prevCameraPitch == null || transitionTotalTicks == 0) return targetCameraPitch;
        return lerp(prevCameraPitch, targetCameraPitch, getTransitionProgress());
    }

    /**
     * Gets the transition progress (0.0 to 1.0).
     */
    private float getTransitionProgress() {
        if (transitionTotalTicks == 0) return 1.0f;
        float progress = (float) transitionTicksElapsed / transitionTotalTicks;
        // Use smoothstep for smoother easing
        return smoothstep(progress);
    }

    /**
     * Smoothstep function for easing.
     */
    private float smoothstep(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /**
     * Linear interpolation for doubles.
     */
    private double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Linear interpolation for floats.
     */
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Angle interpolation (handles wrap-around).
     */
    private float lerpAngle(float a, float b, float t) {
        // Normalize angles to -180 to 180
        float diff = ((b - a + 180) % 360) - 180;
        if (diff < -180) diff += 360;
        return a + diff * t;
    }

    /**
     * Sets the target camera position for this frame.
     * Stores the current target as previous for interpolation.
     *
     * @param transitionTicks number of ticks to transition (use fadeIn)
     */
    public void setTargetCameraPosition(Double x, Double y, Double z, Float yaw, Float pitch, int transitionTicks) {
        // Store current as previous (for interpolation)
        this.prevCameraX = this.targetCameraX;
        this.prevCameraY = this.targetCameraY;
        this.prevCameraZ = this.targetCameraZ;
        this.prevCameraYaw = this.targetCameraYaw;
        this.prevCameraPitch = this.targetCameraPitch;

        // Set new target
        this.targetCameraX = x;
        this.targetCameraY = y;
        this.targetCameraZ = z;
        this.targetCameraYaw = yaw;
        this.targetCameraPitch = pitch;

        // Reset transition
        this.transitionTotalTicks = transitionTicks;
        this.transitionTicksElapsed = 0;
    }

    /**
     * Clears the camera position.
     */
    public void clearCameraPosition() {
        this.targetCameraX = null;
        this.targetCameraY = null;
        this.targetCameraZ = null;
        this.targetCameraYaw = null;
        this.targetCameraPitch = null;
        this.prevCameraX = null;
        this.prevCameraY = null;
        this.prevCameraZ = null;
        this.prevCameraYaw = null;
        this.prevCameraPitch = null;
        this.transitionTotalTicks = 0;
        this.transitionTicksElapsed = 0;
    }

    /**
     * Checks if the frame action has been executed this frame.
     */
    public boolean isFrameActionExecuted() {
        return frameActionExecuted;
    }

    /**
     * Marks the frame action as executed.
     */
    public void setFrameActionExecuted(boolean executed) {
        this.frameActionExecuted = executed;
    }

    /**
     * Advances to the next frame.
     *
     * @param cinematic the cinematic to get the next frame from
     * @return true if there is a next frame, false if the cinematic is complete
     */
    public boolean advanceFrame(TutorialCinematic cinematic) {
        currentFrameIndex++;
        frameActionExecuted = false;

        if (currentFrameIndex >= cinematic.getFrameCount()) {
            return false; // Cinematic complete
        }

        CinematicFrame nextFrame = cinematic.getFrame(currentFrameIndex);
        if (nextFrame != null) {
            frameTicksRemaining = nextFrame.getTotalTime();
        }
        return true;
    }

    /**
     * Initializes the state with the first frame.
     *
     * @param cinematic the cinematic to get the first frame from
     * @return true if initialization successful, false if no frames
     */
    public boolean initFirstFrame(TutorialCinematic cinematic) {
        if (!cinematic.hasFrames()) {
            return false;
        }

        currentFrameIndex = 0;
        frameActionExecuted = false;
        CinematicFrame firstFrame = cinematic.getFrame(0);
        if (firstFrame != null) {
            frameTicksRemaining = firstFrame.getTotalTime();
        }
        return true;
    }
}
