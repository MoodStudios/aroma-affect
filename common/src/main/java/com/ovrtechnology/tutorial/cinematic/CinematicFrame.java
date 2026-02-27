package com.ovrtechnology.tutorial.cinematic;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a single frame in a cinematic sequence.
 * <p>
 * Each frame defines a camera position and can display a title and/or subtitle,
 * play a sound, and trigger an Oliver action.
 *
 * @param title        the title text to display (can be null)
 * @param subtitle     the subtitle text to display (can be null)
 * @param titleColor   the color of the title in hex (e.g., 0xFFFFFF for white)
 * @param duration     the duration in ticks (20 ticks = 1 second)
 * @param fadeIn       the fade-in time in ticks
 * @param fadeOut      the fade-out time in ticks
 * @param sound        the sound ID to play (can be null)
 * @param oliverAction the Oliver action to execute during this frame (can be null)
 * @param cameraX      the camera X position (can be null if not set)
 * @param cameraY      the camera Y position (can be null if not set)
 * @param cameraZ      the camera Z position (can be null if not set)
 * @param cameraYaw    the camera yaw rotation (can be null if not set)
 * @param cameraPitch  the camera pitch rotation (can be null if not set)
 */
public record CinematicFrame(
        @Nullable String title,
        @Nullable String subtitle,
        int titleColor,
        int duration,
        int fadeIn,
        int fadeOut,
        @Nullable String sound,
        @Nullable String oliverAction,
        @Nullable Double cameraX,
        @Nullable Double cameraY,
        @Nullable Double cameraZ,
        @Nullable Float cameraYaw,
        @Nullable Float cameraPitch
) {
    /**
     * Default title color (white).
     */
    public static final int DEFAULT_COLOR = 0xFFFFFF;

    /**
     * Default fade-in time in ticks.
     */
    public static final int DEFAULT_FADE_IN = 10;

    /**
     * Default fade-out time in ticks.
     */
    public static final int DEFAULT_FADE_OUT = 10;

    /**
     * Default duration in ticks (3 seconds).
     */
    public static final int DEFAULT_DURATION = 60;

    /**
     * Creates a frame with default values.
     *
     * @param duration the duration in ticks
     */
    public CinematicFrame(int duration) {
        this(null, null, DEFAULT_COLOR, duration, DEFAULT_FADE_IN, DEFAULT_FADE_OUT, null, null, null, null, null, null, null);
    }

    /**
     * Creates a copy of this frame with a new title.
     */
    public CinematicFrame withTitle(@Nullable String title) {
        return new CinematicFrame(title, subtitle, titleColor, duration, fadeIn, fadeOut, sound, oliverAction, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch);
    }

    /**
     * Creates a copy of this frame with a new subtitle.
     */
    public CinematicFrame withSubtitle(@Nullable String subtitle) {
        return new CinematicFrame(title, subtitle, titleColor, duration, fadeIn, fadeOut, sound, oliverAction, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch);
    }

    /**
     * Creates a copy of this frame with a new title color.
     */
    public CinematicFrame withTitleColor(int titleColor) {
        return new CinematicFrame(title, subtitle, titleColor, duration, fadeIn, fadeOut, sound, oliverAction, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch);
    }

    /**
     * Creates a copy of this frame with a new duration.
     */
    public CinematicFrame withDuration(int duration) {
        return new CinematicFrame(title, subtitle, titleColor, duration, fadeIn, fadeOut, sound, oliverAction, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch);
    }

    /**
     * Creates a copy of this frame with new fade values.
     */
    public CinematicFrame withFades(int fadeIn, int fadeOut) {
        return new CinematicFrame(title, subtitle, titleColor, duration, fadeIn, fadeOut, sound, oliverAction, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch);
    }

    /**
     * Creates a copy of this frame with a new sound.
     */
    public CinematicFrame withSound(@Nullable String sound) {
        return new CinematicFrame(title, subtitle, titleColor, duration, fadeIn, fadeOut, sound, oliverAction, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch);
    }

    /**
     * Creates a copy of this frame with a new Oliver action.
     */
    public CinematicFrame withOliverAction(@Nullable String oliverAction) {
        return new CinematicFrame(title, subtitle, titleColor, duration, fadeIn, fadeOut, sound, oliverAction, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch);
    }

    /**
     * Creates a copy of this frame with a new camera position.
     */
    public CinematicFrame withCameraPosition(double x, double y, double z, float yaw, float pitch) {
        return new CinematicFrame(title, subtitle, titleColor, duration, fadeIn, fadeOut, sound, oliverAction, x, y, z, yaw, pitch);
    }

    /**
     * Creates a copy of this frame with cleared camera position.
     */
    public CinematicFrame withoutCameraPosition() {
        return new CinematicFrame(title, subtitle, titleColor, duration, fadeIn, fadeOut, sound, oliverAction, null, null, null, null, null);
    }

    /**
     * Checks if this frame has any title or subtitle.
     */
    public boolean hasText() {
        return (title != null && !title.isEmpty()) || (subtitle != null && !subtitle.isEmpty());
    }

    /**
     * Checks if this frame has a camera position defined.
     */
    public boolean hasCameraPosition() {
        return cameraX != null && cameraY != null && cameraZ != null;
    }

    /**
     * Gets the total frame time including fades.
     */
    public int getTotalTime() {
        return fadeIn + duration + fadeOut;
    }
}
