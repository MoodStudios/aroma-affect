package com.ovrtechnology.tutorial.waypoint.client;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side renderer for tutorial waypoint paths.
 * <p>
 * Renders an animated particle pulse along multi-point paths.
 * Supports curved paths through multiple waypoints using Catmull-Rom interpolation.
 */
public final class TutorialWaypointRenderer {

    // Rendering configuration
    private static final double PULSE_SPEED = 12.0;             // Blocks per second
    private static final double PULSE_LENGTH = 6.0;             // Length of the glowing pulse
    private static final int PULSE_PARTICLES_PER_BLOCK = 4;     // Density of pulse particles
    private static final double RENDER_DISTANCE_SQ = 80 * 80;   // Max render distance squared
    private static final int TICK_INTERVAL = 1;                 // Render every tick for smoother animation
    private static final double ARC_HEIGHT = 1.2;               // Height of arc above ground
    private static final int CURVE_SEGMENTS = 10;               // Segments per waypoint for smooth curves

    // Purple dust particle (medium purple color)
    // Color format: 0xAARRGGBB - OVR Purple
    private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(
            0xFFA890F0,  // OVR Purple (RGB: 168, 144, 240)
            1.0f         // Medium size (more visible)
    );

    // Active waypoint state
    @Nullable
    private static String activeWaypointId = null;
    private static List<Vec3> pathPoints = new ArrayList<>();   // Interpolated curve points
    private static double totalPathLength = 0.0;

    // Second pulse delay in seconds
    private static final double SECOND_PULSE_DELAY = 2.0;

    // Animation state
    private static double pulseProgress = 0.0;  // 0.0 to 1.0, position along the path
    private static int tickCounter = 0;
    private static boolean initialized = false;

    private TutorialWaypointRenderer() {
    }

    /**
     * Initializes the waypoint renderer.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientTickEvent.CLIENT_POST.register(TutorialWaypointRenderer::onClientTick);
        AromaAffect.LOGGER.debug("Tutorial waypoint renderer initialized");
    }

    /**
     * Sets the active waypoint with multiple positions.
     *
     * @param id        the waypoint ID
     * @param positions list of BlockPos positions in order
     */
    public static void setActiveWaypoint(String id, List<BlockPos> positions) {
        if (positions == null || positions.size() < 2) {
            clearActiveWaypoint();
            return;
        }

        activeWaypointId = id;

        // Convert to Vec3 and generate smooth curve
        List<Vec3> controlPoints = new ArrayList<>();
        for (BlockPos pos : positions) {
            controlPoints.add(Vec3.atCenterOf(pos));
        }

        // Generate interpolated path using Catmull-Rom spline
        pathPoints = generateSmoothPath(controlPoints);
        totalPathLength = calculatePathLength(pathPoints);
        pulseProgress = 0.0;

        // Set the hologram target to the endpoint
        if (!pathPoints.isEmpty()) {
            TutorialArrowHologram.setTarget(pathPoints.get(pathPoints.size() - 1));
        }

        AromaAffect.LOGGER.debug("Waypoint renderer activated: {} ({} points, {:.1f} blocks)",
                id, positions.size(), totalPathLength);
    }

    /**
     * Clears the active waypoint.
     */
    public static void clearActiveWaypoint() {
        activeWaypointId = null;
        pathPoints.clear();
        totalPathLength = 0.0;
        pulseProgress = 0.0;

        // Clear the hologram
        TutorialArrowHologram.clear();

        AromaAffect.LOGGER.debug("Waypoint renderer cleared");
    }

    /**
     * Checks if a waypoint is currently active.
     */
    public static boolean hasActiveWaypoint() {
        return activeWaypointId != null && !pathPoints.isEmpty();
    }

    private static void onClientTick(Minecraft minecraft) {
        if (!hasActiveWaypoint()) {
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

        // Check render distance from player to path
        Vec3 playerPos = player.position();
        if (!isPlayerNearPath(playerPos)) {
            return;
        }

        // Update pulse animation
        if (totalPathLength < 1.0) {
            return;
        }

        // Advance pulse
        double progressIncrement = (PULSE_SPEED * TICK_INTERVAL) / (20.0 * totalPathLength);
        pulseProgress += progressIncrement;
        if (pulseProgress > 1.0 + (PULSE_LENGTH / totalPathLength)) {
            pulseProgress = 0.0;
        }

        // Render the path
        renderPath(level);
    }

    private static void renderPath(ClientLevel level) {
        if (pathPoints.size() < 2) return;

        // Render first pulse
        renderPulseAt(level, pulseProgress);

        // Render second pulse offset by SECOND_PULSE_DELAY seconds behind the first
        double secondPulseOffset = (PULSE_SPEED * SECOND_PULSE_DELAY) / totalPathLength;
        double secondPulseProgress = pulseProgress - secondPulseOffset;
        if (secondPulseProgress >= 0.0) {
            renderPulseAt(level, secondPulseProgress);
        }

        // Render start marker only (end is rendered by TutorialArrowHologram)
        renderEndpointMarker(level, pathPoints.get(0), true);
    }

    private static void renderPulseAt(ClientLevel level, double progress) {
        double pulseStartDist = progress * totalPathLength;
        double pulseEndDist = pulseStartDist + PULSE_LENGTH;

        double accumulatedDistance = 0;

        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Vec3 start = pathPoints.get(i);
            Vec3 end = pathPoints.get(i + 1);
            double segmentLength = start.distanceTo(end);

            double segmentStart = accumulatedDistance;
            double segmentEnd = accumulatedDistance + segmentLength;

            // Check if pulse overlaps this segment
            if (pulseEndDist > segmentStart && pulseStartDist < segmentEnd) {
                // Calculate overlap
                double overlapStart = Math.max(pulseStartDist, segmentStart);
                double overlapEnd = Math.min(pulseEndDist, segmentEnd);

                int particleCount = (int) ((overlapEnd - overlapStart) * PULSE_PARTICLES_PER_BLOCK);

                for (int j = 0; j <= particleCount; j++) {
                    double dist = overlapStart + (overlapEnd - overlapStart) * ((double) j / Math.max(1, particleCount));
                    double t = (dist - segmentStart) / segmentLength;
                    Vec3 pos = start.lerp(end, t);

                    // Arc effect - higher in the middle of the path, lower at ends
                    double globalT = dist / totalPathLength;
                    double arcHeight = ARC_HEIGHT * Math.sin(globalT * Math.PI);

                    // Pulse wave (subtle)
                    double pulseT = (dist - pulseStartDist) / PULSE_LENGTH;
                    double pulseWave = Math.sin(pulseT * Math.PI) * 0.1;

                    double offsetY = 0.5 + arcHeight + pulseWave;

                    // Yellow dust particle (thin line)
                    level.addParticle(
                            PURPLE_DUST,
                            pos.x,
                            pos.y + offsetY,
                            pos.z,
                            0, 0, 0
                    );
                }
            }

            accumulatedDistance += segmentLength;
        }
    }

    private static void renderEndpointMarker(ClientLevel level, Vec3 pos, boolean isStart) {
        if (isStart) {
            // Start point: small purple sparkle ring
            double time = System.currentTimeMillis() / 1000.0;
            for (int i = 0; i < 4; i++) {
                double angle = time * 2.0 + (Math.PI * 0.5 * i);
                double radius = 0.3;
                level.addParticle(
                        PURPLE_DUST,
                        pos.x + Math.cos(angle) * radius,
                        pos.y + 0.8,
                        pos.z + Math.sin(angle) * radius,
                        0, 0.02, 0
                );
            }
        }
        // End point is now rendered by TutorialArrowHologram (UTF-8 3D text)
    }

    /**
     * Generates a smooth path using Catmull-Rom spline interpolation.
     */
    private static List<Vec3> generateSmoothPath(List<Vec3> controlPoints) {
        if (controlPoints.size() < 2) {
            return new ArrayList<>(controlPoints);
        }

        List<Vec3> smoothPath = new ArrayList<>();

        for (int i = 0; i < controlPoints.size() - 1; i++) {
            // Get 4 control points for Catmull-Rom (with clamping at edges)
            Vec3 p0 = controlPoints.get(Math.max(0, i - 1));
            Vec3 p1 = controlPoints.get(i);
            Vec3 p2 = controlPoints.get(i + 1);
            Vec3 p3 = controlPoints.get(Math.min(controlPoints.size() - 1, i + 2));

            // Generate points along this segment
            for (int j = 0; j < CURVE_SEGMENTS; j++) {
                double t = (double) j / CURVE_SEGMENTS;
                smoothPath.add(catmullRom(p0, p1, p2, p3, t));
            }
        }

        // Add final point
        smoothPath.add(controlPoints.get(controlPoints.size() - 1));

        return smoothPath;
    }

    /**
     * Catmull-Rom spline interpolation.
     */
    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;

        double x = 0.5 * ((2 * p1.x) +
                (-p0.x + p2.x) * t +
                (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 +
                (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);

        double y = 0.5 * ((2 * p1.y) +
                (-p0.y + p2.y) * t +
                (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
                (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);

        double z = 0.5 * ((2 * p1.z) +
                (-p0.z + p2.z) * t +
                (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 +
                (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);

        return new Vec3(x, y, z);
    }

    private static double calculatePathLength(List<Vec3> points) {
        double total = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            total += points.get(i).distanceTo(points.get(i + 1));
        }
        return total;
    }

    private static boolean isPlayerNearPath(Vec3 playerPos) {
        for (Vec3 point : pathPoints) {
            if (playerPos.distanceToSqr(point) < RENDER_DISTANCE_SQ) {
                return true;
            }
        }
        return false;
    }
}
