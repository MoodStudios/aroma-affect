package com.ovrtechnology.tutorial.cinematic.client;

/**
 * Client-side flag to lock camera rotation during cinematics.
 * Set via S2C packet when cinematic starts/stops.
 */
public final class CinematicCameraLock {

    private static boolean locked = false;

    private CinematicCameraLock() {}

    public static boolean isLocked() {
        return locked;
    }

    public static void setLocked(boolean lock) {
        locked = lock;
    }
}
