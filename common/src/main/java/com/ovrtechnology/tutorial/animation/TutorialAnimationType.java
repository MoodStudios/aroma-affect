package com.ovrtechnology.tutorial.animation;

/**
 * Types of tutorial animations for dramatic "path opening" moments.
 */
public enum TutorialAnimationType {

    /**
     * Blocks explode outward with debris particles and dust clouds.
     * Layers removed from center outward (explosion pattern).
     */
    WALL_BREAK,

    /**
     * Blocks slide apart to the sides with swoosh particles.
     * Layers removed from center splitting left/right.
     */
    DOOR_OPEN,

    /**
     * Blocks dissolve/disintegrate with sparkle particles rising upward.
     * Layers removed from top to bottom.
     */
    DEBRIS_CLEAR;

    /**
     * Gets an animation type by name (case-insensitive).
     *
     * @param name the name
     * @return the type, or null if not found
     */
    public static TutorialAnimationType byName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
