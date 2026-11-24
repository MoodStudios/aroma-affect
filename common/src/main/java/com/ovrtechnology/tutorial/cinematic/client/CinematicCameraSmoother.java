package com.ovrtechnology.tutorial.cinematic.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Smooths the camera of a cinematic.
 * <p>
 * The server moves the camera by teleporting the player once per tick, and a teleport is
 * applied on the client as a jump, so at 60 fps the picture advances in 20 steps a second.
 * This keeps the last two camera positions, one per client tick, and
 * {@link com.ovrtechnology.mixin.CameraMixin} draws every frame somewhere between them.
 * The cost is that the picture runs one tick (50 ms) behind the server.
 */
public final class CinematicCameraSmoother {

    /** A move longer than this in one tick is a cut to another shot, not travel. */
    private static final double CUT_DISTANCE = 4.0;

    private static boolean initialized = false;

    private static boolean tracking = false;
    private static double fromX, fromY, fromZ;
    private static float fromYaw, fromPitch;
    private static double toX, toY, toZ;
    private static float toYaw, toPitch;

    private CinematicCameraSmoother() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientTickEvent.CLIENT_POST.register(CinematicCameraSmoother::sample);
    }

    private static void sample(Minecraft mc) {
        if (!CinematicCameraLock.isLocked() || mc.player == null) {
            tracking = false;
            return;
        }

        double x = mc.player.getX();
        double y = mc.player.getEyeY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        double dx = x - toX, dy = y - toY, dz = z - toZ;
        boolean cut = !tracking || dx * dx + dy * dy + dz * dz > CUT_DISTANCE * CUT_DISTANCE;
        if (cut) {
            fromX = x;
            fromY = y;
            fromZ = z;
            fromYaw = yaw;
            fromPitch = pitch;
        } else {
            fromX = toX;
            fromY = toY;
            fromZ = toZ;
            fromYaw = toYaw;
            fromPitch = toPitch;
        }
        toX = x;
        toY = y;
        toZ = z;
        toYaw = yaw;
        toPitch = pitch;
        tracking = true;
    }

    /** True while there is a cinematic camera to draw from. */
    public static boolean isActive() {
        return tracking && CinematicCameraLock.isLocked();
    }

    public static double x(float partialTick) {
        return Mth.lerp(partialTick, fromX, toX);
    }

    public static double y(float partialTick) {
        return Mth.lerp(partialTick, fromY, toY);
    }

    public static double z(float partialTick) {
        return Mth.lerp(partialTick, fromZ, toZ);
    }

    public static float yaw(float partialTick) {
        return Mth.rotLerp(partialTick, fromYaw, toYaw);
    }

    public static float pitch(float partialTick) {
        return Mth.lerp(partialTick, fromPitch, toPitch);
    }
}
