package com.ovrtechnology.tutorial.chest.client;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side renderer for tutorial chest particle effects.
 * <p>
 * Renders a floating "?" question mark above unconsumed tutorial chests.
 * The question mark is purple (OVR Purple) and rotates slowly.
 */
public final class TutorialChestRenderer {

    // Rendering configuration
    private static final double RENDER_DISTANCE_SQ = 50 * 50;   // Max render distance squared
    private static final int TICK_INTERVAL = 2;                  // Render every 2 ticks
    private static final double FLOAT_HEIGHT = 1.5;              // Height above chest
    private static final double FLOAT_AMPLITUDE = 0.1;           // Floating animation amplitude
    private static final double FLOAT_SPEED = 2.0;               // Floating animation speed

    // Purple dust particle (OVR Purple color)
    private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(
            0xFFA890F0,  // OVR Purple (RGB: 168, 144, 240)
            0.8f         // Size
    );

    // Gold accent for the dot
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(
            0xFFFFD700,  // Gold
            0.6f         // Smaller size for dot
    );

    // Active chest positions
    private static final Set<BlockPos> chestPositions = new HashSet<>();

    // Animation state
    private static int tickCounter = 0;
    private static boolean initialized = false;

    private TutorialChestRenderer() {
    }

    /**
     * Initializes the chest renderer.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientTickEvent.CLIENT_POST.register(client -> {
            if (ReplayCompat.isInReplay()) return;
            onClientTick(client);
        });
        AromaAffect.LOGGER.debug("Tutorial chest renderer initialized");
    }

    /**
     * Sets the chest positions to render.
     *
     * @param positions array of chest positions
     */
    public static void setChestPositions(BlockPos[] positions) {
        chestPositions.clear();
        if (positions != null) {
            for (BlockPos pos : positions) {
                chestPositions.add(pos);
            }
        }
        AromaAffect.LOGGER.debug("Chest renderer: {} positions set", chestPositions.size());
    }

    /**
     * Removes a single chest position.
     *
     * @param position the position to remove
     */
    public static void removeChestPosition(BlockPos position) {
        chestPositions.remove(position);
        AromaAffect.LOGGER.debug("Chest renderer: removed position {}", position);
    }

    /**
     * Clears all chest positions.
     */
    public static void clearChestPositions() {
        chestPositions.clear();
        AromaAffect.LOGGER.debug("Chest renderer: cleared all positions");
    }

    private static void onClientTick(Minecraft minecraft) {
        if (chestPositions.isEmpty()) {
            return;
        }

        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        // Throttle rendering
        tickCounter++;
        if (tickCounter < TICK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        Vec3 playerPos = player.position();

        // Render each chest
        for (BlockPos chestPos : chestPositions) {
            Vec3 chestVec = Vec3.atCenterOf(chestPos);
            if (playerPos.distanceToSqr(chestVec) > RENDER_DISTANCE_SQ) {
                continue;
            }

            renderQuestionMark(level, chestPos);
        }
    }

    /**
     * Renders a question mark above a chest position.
     * The question mark pattern:
     * <pre>
     *    ●●●
     *   ●   ●
     *       ●
     *      ●
     *
     *      ●
     * </pre>
     */
    private static void renderQuestionMark(ClientLevel level, BlockPos chestPos) {
        double time = System.currentTimeMillis() / 1000.0;

        // Floating animation
        double floatOffset = Math.sin(time * FLOAT_SPEED) * FLOAT_AMPLITUDE;

        // Slow rotation
        double rotation = time * 0.5;

        double centerX = chestPos.getX() + 0.5;
        double centerZ = chestPos.getZ() + 0.5;
        double baseY = chestPos.getY() + FLOAT_HEIGHT + floatOffset;

        // Scale of the question mark
        double scale = 0.15;

        // ═══════════════════════════════════════════════════════════════
        // Question mark shape (relative coordinates, scaled)
        // ═══════════════════════════════════════════════════════════════

        // Top curve of ?
        double[][] topCurve = {
                {-1, 4},    // Left top
                {0, 4.5},   // Top center
                {1, 4},     // Right top
                {1, 3},     // Right side going down
                {0, 2.5},   // Curve to center
                {0, 2},     // Going down
                {0, 1.5},   // Bottom of curve
        };

        for (double[] point : topCurve) {
            double localX = point[0] * scale;
            double localZ = 0;
            double localY = point[1] * scale;

            // Apply rotation
            double rotatedX = localX * Math.cos(rotation) - localZ * Math.sin(rotation);
            double rotatedZ = localX * Math.sin(rotation) + localZ * Math.cos(rotation);

            level.addParticle(
                    PURPLE_DUST,
                    centerX + rotatedX,
                    baseY + localY,
                    centerZ + rotatedZ,
                    0, 0, 0
            );
        }

        // Dot at the bottom
        double dotY = baseY + 0.5 * scale;
        level.addParticle(
                GOLD_DUST,
                centerX,
                dotY,
                centerZ,
                0, 0, 0
        );

        // Add some sparkle particles around it
        if (Math.random() < 0.3) {
            double sparkleAngle = Math.random() * Math.PI * 2;
            double sparkleRadius = 0.2 + Math.random() * 0.3;
            double sparkleY = baseY + Math.random() * 0.5;

            level.addParticle(
                    PURPLE_DUST,
                    centerX + Math.cos(sparkleAngle) * sparkleRadius,
                    sparkleY,
                    centerZ + Math.sin(sparkleAngle) * sparkleRadius,
                    0, 0.02, 0
            );
        }
    }
}
