package com.ovrtechnology.compat;

import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;

/**
 * Compatibility layer for replay mods (Flashback, ReplayMod).
 * <p>
 * Detects when the game is playing back a replay so that AromaAffect
 * can disable tick handlers, player-join logic, networking, and the
 * WebSocket client — all of which interfere with replay playback.
 * <p>
 * Uses reflection exclusively so Flashback is NOT required at compile time.
 */
public final class ReplayCompat {

    private static final boolean FLASHBACK_LOADED;
    private static Method isInReplayMethod;
    private static Class<?> replayServerClass;

    static {
        FLASHBACK_LOADED = isClassPresent("com.moulberry.flashback.Flashback");
        if (FLASHBACK_LOADED) {
            try {
                Class<?> flashbackClass = Class.forName("com.moulberry.flashback.Flashback");
                isInReplayMethod = flashbackClass.getMethod("isInReplay");
            } catch (Exception ignored) {
            }
            try {
                replayServerClass = Class.forName("com.moulberry.flashback.playback.ReplayServer");
            } catch (Exception ignored) {
            }
        }
    }

    private ReplayCompat() {
    }

    /**
     * Returns {@code true} when the client is currently playing back a
     * Flashback replay.  Safe to call from any thread — returns
     * {@code false} when Flashback is not installed.
     */
    public static boolean isInReplay() {
        if (!FLASHBACK_LOADED || isInReplayMethod == null) {
            return false;
        }
        try {
            return (boolean) isInReplayMethod.invoke(null);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Returns {@code true} when the given server is a Flashback
     * {@code ReplayServer}.  Safe to call from server tick handlers.
     */
    public static boolean isReplayServer(MinecraftServer server) {
        if (!FLASHBACK_LOADED || replayServerClass == null || server == null) {
            return false;
        }
        try {
            return replayServerClass.isInstance(server);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, ReplayCompat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
