package com.ovrtechnology.guide;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Calculates the compass needle angle that points toward the nearest village.
 * <p>
 * The angle is normalized to [0, 1) where 0 = north and rotates clockwise.
 * This is consumed by the item model via a {@code compass}-style property override.
 */
public final class AromaGuideCompassBehavior {

    private static float currentAngle = 0f;
    private static float previousAngle = 0f;

    private AromaGuideCompassBehavior() {}

    /**
     * Computes the smoothly interpolated compass angle for rendering.
     *
     * @param entity the holder entity
     * @param level  the client level
     * @return a value in [0, 1) representing the needle direction
     */
    public static float getAngle(Entity entity, ClientLevel level) {
        if (!(entity instanceof Player player)) {
            return 0f;
        }

        BlockPos target = AromaGuideTracker.getCompassTargetPos();
        if (target == null) {
            // Spin slowly when no village is found
            previousAngle = currentAngle;
            currentAngle += 0.01f;
            if (currentAngle > 1f) currentAngle -= 1f;
            return currentAngle;
        }

        // Angle from player to target in radians (0 = south, increases counter-clockwise)
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double angleToTarget = Math.atan2(dz, dx);

        // Player's body rotation in radians
        double playerAngle = Math.toRadians(player.getYRot());

        // Relative angle: how much the target deviates from where the player faces
        double relative = angleToTarget - playerAngle;

        // Normalize to [0, 1)
        float targetNormalized = (float) (0.5 - (relative / (2 * Math.PI)));

        // Smooth interpolation to avoid jitter
        float delta = targetNormalized - previousAngle;
        // Wrap delta into [-0.5, 0.5]
        delta -= Math.floor(delta + 0.5f);

        previousAngle += delta * 0.1f;
        previousAngle -= Math.floor(previousAngle);
        currentAngle = previousAngle;

        return currentAngle;
    }
}
