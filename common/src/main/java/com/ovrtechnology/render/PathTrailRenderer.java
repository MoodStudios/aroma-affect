package com.ovrtechnology.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.ovrtechnology.block.BlockRegistry;
import com.ovrtechnology.menu.ActiveTrackingState;
import com.ovrtechnology.menu.MenuCategory;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Client-side X-ray trail renderer that draws a glowing path from the player
 * to the tracked destination, visible through walls.
 * <p>
 * The trail uses a pulse-reveal system: a bright wave periodically fires from
 * the DESTINATION toward the player. As the wave sweeps back, it BUILDS
 * the path behind it. Every Nth pulse is a "power pulse" with enhanced visuals.
 */
public final class PathTrailRenderer {

    // ── Trail shape parameters ──────────────────────────────────────────

    private static final double SAMPLE_SPACING = 1.5;
    private static final double HEIGHT_OFFSET = 0.6;
    private static final double MAX_RENDER_DISTANCE = 120.0;
    private static final double MAX_Y_STEP = 1.5;

    // Dual sine wave parameters (organic worm-like undulation)
    private static final double WAVE1_AMPLITUDE = 0.5;
    private static final double WAVE1_FREQUENCY = 0.12;
    private static final double WAVE2_AMPLITUDE = 0.15;
    private static final double WAVE2_FREQUENCY = 0.324;
    private static final double WAVE2_PHASE_OFFSET = 1.3;

    // ── Pulse timing ────────────────────────────────────────────────────

    /** Pulse duration bounds; actual duration is dynamic based on distance. */
    private static final long PULSE_DURATION_NEAR_MS = 700;
    private static final long PULSE_DURATION_FAR_MS = 2500;
    /** Pulse cycle bounds; keeps rhythm while still allowing faster near-target sweeps. */
    private static final long PULSE_CYCLE_NEAR_MS = 1700;
    private static final long PULSE_CYCLE_FAR_MS = 4000;
    /** Breathing animation period. */
    private static final long BREATHE_PERIOD_MS = 3000;

    // ── Pulse visibility ────────────────────────────────────────────────

    /** How long each trail section stays visible after the pulse passes. */
    private static final long WAKE_VISIBLE_MS = 2500;
    /** How long the fade-out takes after the visible period ends. */
    private static final long WAKE_FADE_MS = 1000;

    // ── Pulse visual parameters (normal) ────────────────────────────────

    /** Width (in blocks) of the bright leading-edge flash. */
    private static final double LEADING_EDGE_WIDTH = 6.0;
    /** Width (in blocks) of elevated brightness behind the leading edge. */
    private static final double WAKE_GLOW_WIDTH = 20.0;

    // ── Power pulse parameters ──────────────────────────────────────────

    /** Every Nth pulse is a power pulse (configurable). */
    private static final int POWER_PULSE_INTERVAL = 5;

    private static final double POWER_LEADING_EDGE_WIDTH = 10.0;
    private static final double POWER_WAKE_GLOW_WIDTH = 35.0;

    // ── Two-path state (stable + incoming revealed by pulse) ────────────

    private static List<Vec3> stablePath = new ArrayList<>();
    private static List<Vec3> incomingPath = null;
    private static List<Vec3> prevStablePath = null;
    private static BlockPos cachedDest = null;
    private static long lastPulseStart = 0;
    private static long prevPulseStart = 0;
    private static long currentPulseDurationMs = PULSE_DURATION_FAR_MS;
    private static long prevPulseDurationMs = PULSE_DURATION_FAR_MS;

    // ── Player-tracking: where the player was when each path was computed ──

    private static Vec3 stablePathOrigin = null;
    private static Vec3 incomingPathOrigin = null;
    private static Vec3 prevPathOrigin = null;

    // ── Power pulse tracking ────────────────────────────────────────────

    private static int pulseCount = 0;
    private static boolean currentPulseIsPower = false;
    private static boolean prevPulseIsPower = false;

    // ── Client-side particle spawning ───────────────────────────────────
    private static final long PARTICLE_SPAWN_INTERVAL_MS = 250;
    private static long lastParticleSpawnTime = 0;

    // ── Pipeline (shared by both glow and core render types) ────────────

    private static final RenderPipeline TRAIL_PIPELINE = RenderPipeline.builder()
            .withLocation("aromaaffect/pipeline/trail_no_depth")
            .withVertexShader("core/rendertype_lines")
            .withFragmentShader("core/rendertype_lines")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES)
            .build();

    // ── Render types (different line widths) ─────────────────────────────

    private static final RenderType TRAIL_GLOW = TrailRenderType.create("aromaaffect_trail_glow", 5.0);
    private static final RenderType TRAIL_CORE = TrailRenderType.create("aromaaffect_trail_core", 1.5);

    private static final RenderType TRAIL_GLOW_POWER = TrailRenderType.create("aromaaffect_trail_glow_power", 8.0);
    private static final RenderType TRAIL_CORE_POWER = TrailRenderType.create("aromaaffect_trail_core_power", 2.5);

    private PathTrailRenderer() {}

    /**
     * Returns a stable visual anchor near the player's nose/head so the
     * incoming trail ends where the player perceives the scent intake.
     */
    private static Vec3 getPlayerTrailAnchor(net.minecraft.client.player.LocalPlayer player) {
        // Slightly below eye line feels closer to "nose" than exact eye center.
        return player.getEyePosition().add(0.0, -0.18, 0.0);
    }

    // ── Public entry point ──────────────────────────────────────────────

    public static void renderTrail(PoseStack poseStack, Vec3 cameraPos, MultiBufferSource consumers) {
        if (!ActiveTrackingState.isActivelyTracking()) return;

        BlockPos dest = ActiveTrackingState.getDestination();
        if (dest == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 playerPos = getPlayerTrailAnchor(mc.player);
        long now = System.currentTimeMillis();

        // ── Destination changed → reset everything ──
        if (!dest.equals(cachedDest)) {
            stablePath = computeTrailPoints(playerPos, dest, mc.level);
            stablePathOrigin = playerPos;
            cachedDest = dest;
            incomingPath = new ArrayList<>(stablePath);
            incomingPathOrigin = playerPos;
            prevStablePath = null;
            prevPathOrigin = null;
            prevPulseStart = 0;
            prevPulseDurationMs = PULSE_DURATION_FAR_MS;
            lastPulseStart = now;
            currentPulseDurationMs = resolvePulseDurationMs(totalLength(stablePath));
            pulseCount = 1;
            currentPulseIsPower = false;
            prevPulseIsPower = false;
        }

        // ── Manage pulse lifecycle ──
        long elapsed = now - lastPulseStart;
        boolean pulseActive = incomingPath != null && elapsed < currentPulseDurationMs;

        // Pulse just completed → adopt incoming as stable
        if (!pulseActive && incomingPath != null) {
            stablePath = incomingPath;
            stablePathOrigin = incomingPathOrigin;
            incomingPath = null;
        }

        // Time for a new pulse → recompute path from current player position
        long cycleMs = resolvePulseCycleMs(currentPulseDurationMs);
        if (incomingPath == null && elapsed >= cycleMs) {
            prevStablePath = stablePath;
            prevPathOrigin = stablePathOrigin;
            incomingPath = computeTrailPoints(playerPos, dest, mc.level);
            incomingPathOrigin = playerPos;
            prevPulseStart = lastPulseStart;
            prevPulseDurationMs = currentPulseDurationMs;
            prevPulseIsPower = currentPulseIsPower;
            lastPulseStart = now;
            currentPulseDurationMs = resolvePulseDurationMs(totalLength(incomingPath));
            pulseCount++;
            currentPulseIsPower = (pulseCount % POWER_PULSE_INTERVAL == 0);
            elapsed = 0;
            pulseActive = true;
        }

        // ── Use whichever path is current, adjusted to follow the player ──
        boolean useIncoming = pulseActive && incomingPath != null && !incomingPath.isEmpty();
        List<Vec3> basePath = useIncoming ? incomingPath : stablePath;
        Vec3 baseOrigin = useIncoming ? incomingPathOrigin : stablePathOrigin;
        if (basePath.size() < 2) return;

        List<Vec3> renderPoints = adjustedForPlayer(basePath, baseOrigin, playerPos);

        // ── Color & animation ──
        float[] color = resolveColor();
        float r = color[0], g = color[1], b = color[2];

        double totalLen = totalLength(renderPoints);
        float breathe = 0.85f + 0.15f * (float) Math.sin(
                (now % BREATHE_PERIOD_MS) / (double) BREATHE_PERIOD_MS * Math.PI * 2.0);

        PoseStack.Pose pose = poseStack.last();

        // ── Render previous path fading on its own geometry ──
        if (prevStablePath != null && prevStablePath.size() >= 2 && prevPulseStart > 0) {
            List<Vec3> adjustedPrev = adjustedForPlayer(prevStablePath, prevPathOrigin, playerPos);
            double prevTotalLen = totalLength(adjustedPrev);
            long lastPointReach = prevPulseStart + prevPulseDurationMs;
            if (now - lastPointReach < WAKE_VISIBLE_MS + WAKE_FADE_MS) {
                RenderType prevGlow = prevPulseIsPower ? TRAIL_GLOW_POWER : TRAIL_GLOW;
                RenderType prevCore = prevPulseIsPower ? TRAIL_CORE_POWER : TRAIL_CORE;
                float prevGlowAlpha = prevPulseIsPower ? 0.20f : 0.12f;
                float prevCoreAlpha = prevPulseIsPower ? 0.75f : 0.55f;
                renderLayer(pose, cameraPos, consumers.getBuffer(prevGlow),
                        adjustedPrev, r, g, b, prevGlowAlpha * breathe,
                        now, prevPulseStart, 0, prevPulseDurationMs, prevTotalLen, true, prevPulseIsPower);
                renderLayer(pose, cameraPos, consumers.getBuffer(prevCore),
                        adjustedPrev, r, g, b, prevCoreAlpha * breathe,
                        now, prevPulseStart, 0, prevPulseDurationMs, prevTotalLen, false, prevPulseIsPower);
            } else {
                prevStablePath = null;
                prevPathOrigin = null;
            }
        }

        // ── Render current path (current pulse only) ──
        RenderType curGlow = currentPulseIsPower ? TRAIL_GLOW_POWER : TRAIL_GLOW;
        RenderType curCore = currentPulseIsPower ? TRAIL_CORE_POWER : TRAIL_CORE;
        float curGlowAlpha = currentPulseIsPower ? 0.20f : 0.12f;
        float curCoreAlpha = currentPulseIsPower ? 0.75f : 0.55f;
        renderLayer(pose, cameraPos, consumers.getBuffer(curGlow),
                renderPoints, r, g, b, curGlowAlpha * breathe,
                now, lastPulseStart, 0, currentPulseDurationMs, totalLen, true, currentPulseIsPower);
        renderLayer(pose, cameraPos, consumers.getBuffer(curCore),
                renderPoints, r, g, b, curCoreAlpha * breathe,
                now, lastPulseStart, 0, currentPulseDurationMs, totalLen, false, currentPulseIsPower);

        // ── Client-side particle spawning (synchronized with pulse) ──
        if (now - lastParticleSpawnTime >= PARTICLE_SPAWN_INTERVAL_MS) {
            lastParticleSpawnTime = now;
            spawnPulseParticles(mc.level, renderPoints, totalLen, now, lastPulseStart, currentPulseDurationMs, color, currentPulseIsPower);
            if (prevStablePath != null && prevStablePath.size() >= 2) {
                List<Vec3> adjustedPrevParticles = adjustedForPlayer(prevStablePath, prevPathOrigin, playerPos);
                double prevTotalLen = totalLength(adjustedPrevParticles);
                spawnPulseParticles(mc.level, adjustedPrevParticles, prevTotalLen, now, prevPulseStart, prevPulseDurationMs, color, prevPulseIsPower);
            }
        }
    }

    /**
     * Clears all trail state. Call when tracking stops.
     */
    public static void clearCache() {
        stablePath = new ArrayList<>();
        incomingPath = null;
        prevStablePath = null;
        cachedDest = null;
        lastPulseStart = 0;
        prevPulseStart = 0;
        currentPulseDurationMs = PULSE_DURATION_FAR_MS;
        prevPulseDurationMs = PULSE_DURATION_FAR_MS;
        lastParticleSpawnTime = 0;
        pulseCount = 0;
        currentPulseIsPower = false;
        prevPulseIsPower = false;
        stablePathOrigin = null;
        incomingPathOrigin = null;
        prevPathOrigin = null;
    }

    // ── Trail point computation ─────────────────────────────────────────

    private static List<Vec3> computeTrailPoints(Vec3 playerPos, BlockPos dest, ClientLevel level) {
        double destX = dest.getX() + 0.5;
        double destZ = dest.getZ() + 0.5;
        double destY = dest.getY() + 0.5;

        double dx = destX - playerPos.x;
        double dz = destZ - playerPos.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        if (horizDist < 1.0) return List.of();

        double renderDist = Math.min(horizDist, MAX_RENDER_DISTANCE);
        double dirX = dx / horizDist;
        double dirZ = dz / horizDist;
        double perpX = -dirZ;
        double perpZ = dirX;

        int numPoints = Math.max(10, (int) (renderDist / SAMPLE_SPACING));
        List<Vec3> points = new ArrayList<>(numPoints + 1);

        double smoothedY = playerPos.y;

        for (int i = 0; i <= numPoints; i++) {
            double t = (double) i / numPoints;
            double dist = t * renderDist;

            // Distance from destination (anchored wave phase — stable in world)
            double distFromDest = horizDist - dist;

            // Dual sine waves for organic worm-like undulation
            double wave1 = Math.sin(distFromDest * WAVE1_FREQUENCY) * WAVE1_AMPLITUDE;
            double wave2 = Math.sin(distFromDest * WAVE2_FREQUENCY + WAVE2_PHASE_OFFSET) * WAVE2_AMPLITUDE;
            double wave = wave1 + wave2;

            // Fade amplitude near player and at far end
            double fadeIn = Math.min(1.0, dist / 6.0);
            double fadeOut = Math.min(1.0, (renderDist - dist) / 6.0);
            wave *= fadeIn * fadeOut;

            double baseX = playerPos.x + dirX * dist;
            double baseZ = playerPos.z + dirZ * dist;
            double x = baseX + perpX * wave;
            double z = baseZ + perpZ * wave;

            // Smart Y interpolation: lerp between player and dest
            double interpY = playerPos.y + (destY - playerPos.y) * (dist / horizDist);
            double surfaceY = getGroundHeight(level, (int) x, (int) z) + HEIGHT_OFFSET;
            double targetY = Math.min(interpY, surfaceY);

            // Y-smoothing: clamp vertical change per step
            double yDiff = targetY - smoothedY;
            if (yDiff > MAX_Y_STEP) smoothedY += MAX_Y_STEP;
            else if (yDiff < -MAX_Y_STEP) smoothedY -= MAX_Y_STEP;
            else smoothedY = targetY;

            points.add(new Vec3(x, smoothedY, z));
        }

        return points;
    }

    private static int getGroundHeight(ClientLevel level, int x, int z) {
        try {
            if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
                return level.getSeaLevel();
            }

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
            int minY = Math.max(level.getMinY(), y - 20);

            for (int scanY = y; scanY > minY; scanY--) {
                pos.setY(scanY);
                BlockState below = level.getBlockState(pos);
                pos.setY(scanY + 1);
                BlockState above = level.getBlockState(pos);

                if (isGroundBlock(below) && isPassable(above)) {
                    return scanY + 1;
                }
            }

            return y;
        } catch (Exception e) {
            return level.getSeaLevel();
        }
    }

    private static boolean isGroundBlock(BlockState state) {
        if (!state.isSolid()) return false;
        if (state.is(BlockTags.LOGS)) return false;
        if (state.is(BlockTags.FENCES)) return false;
        if (state.is(BlockTags.WALLS)) return false;
        return true;
    }

    private static boolean isPassable(BlockState state) {
        return state.isAir() || !state.isSolid() || state.is(BlockTags.REPLACEABLE);
    }

    // ── Layer rendering ─────────────────────────────────────────────────

    private static void renderLayer(PoseStack.Pose pose, Vec3 cam, VertexConsumer consumer,
                                     List<Vec3> points, float r, float g, float b,
                                     float baseAlpha, long now, long pulseStart,
                                     long prevPulse, long pulseDurationMs, double totalLen, boolean isGlow,
                                     boolean isPower) {
        double accumulated = 0.0;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i);
            Vec3 next = points.get(i + 1);
            double segLen = a.distanceTo(next);

            float alphaA = computeAlpha(accumulated, totalLen, now, pulseStart, prevPulse, pulseDurationMs, baseAlpha, isGlow, isPower);
            float alphaB = computeAlpha(accumulated + segLen, totalLen, now, pulseStart, prevPulse, pulseDurationMs, baseAlpha, isGlow, isPower);

            if (alphaA < 0.005f && alphaB < 0.005f) {
                accumulated += segLen;
                continue;
            }

            // Base gradient: shifts toward white near destination
            float gradA = (float) (accumulated / totalLen);
            float gradB = (float) ((accumulated + segLen) / totalLen);
            float rA = r + (1.0f - r) * gradA * 0.25f;
            float gA = g + (1.0f - g) * gradA * 0.25f;
            float bA = b + (1.0f - b) * gradA * 0.25f;
            float rB = r + (1.0f - r) * gradB * 0.25f;
            float gB = g + (1.0f - g) * gradB * 0.25f;
            float bB = b + (1.0f - b) * gradB * 0.25f;

            // Pulse leading-edge whitening
            float whitenA = computePulseWhiten(accumulated, now, pulseStart, pulseDurationMs, totalLen, isPower);
            float whitenB = computePulseWhiten(accumulated + segLen, now, pulseStart, pulseDurationMs, totalLen, isPower);
            rA += (1.0f - rA) * whitenA;
            gA += (1.0f - gA) * whitenA;
            bA += (1.0f - bA) * whitenA;
            rB += (1.0f - rB) * whitenB;
            gB += (1.0f - gB) * whitenB;
            bB += (1.0f - bB) * whitenB;

            // Normal = segment direction
            float nx = (float) (next.x - a.x);
            float ny = (float) (next.y - a.y);
            float nz = (float) (next.z - a.z);
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0) { nx /= len; ny /= len; nz /= len; }

            // Camera-relative positions
            float ax = (float) (a.x - cam.x);
            float ay = (float) (a.y - cam.y);
            float az = (float) (a.z - cam.z);
            float bx = (float) (next.x - cam.x);
            float by = (float) (next.y - cam.y);
            float bz = (float) (next.z - cam.z);

            consumer.addVertex(pose, ax, ay, az)
                    .setColor(rA, gA, bA, alphaA)
                    .setNormal(pose, nx, ny, nz);
            consumer.addVertex(pose, bx, by, bz)
                    .setColor(rB, gB, bB, alphaB)
                    .setNormal(pose, nx, ny, nz);

            accumulated += segLen;
        }
    }

    // ── Time-based pulse alpha ──────────────────────────────────────────

    /**
     * Computes alpha for a single pulse given its start time.
     * The pulse sweeps from destination (dist=totalLen) toward the player (dist=0).
     */
    private static float computeAlphaForPulse(double dist, double totalLen,
                                               long now, long pulseStart,
                                               long pulseDurationMs,
                                               float baseAlpha, boolean isGlow,
                                               boolean isPower) {
        if (pulseStart <= 0) return 0;

        // Reversed: pulse starts at dest (dist=totalLen) and sweeps toward player (dist=0)
        double reachTime = pulseStart + ((totalLen - dist) / totalLen) * pulseDurationMs;
        double timeSincePassed = now - reachTime;

        if (timeSincePassed < 0) {
            // Pulse hasn't reached this point yet → invisible
            return 0;
        }

        // Visibility decay: visible for WAKE_VISIBLE_MS, then fades over WAKE_FADE_MS
        float visibility = 1.0f;
        if (timeSincePassed > WAKE_VISIBLE_MS) {
            double fadeTime = timeSincePassed - WAKE_VISIBLE_MS;
            if (fadeTime >= WAKE_FADE_MS) {
                return 0; // Fully faded
            }
            visibility = 1.0f - (float) (fadeTime / WAKE_FADE_MS);
        }

        // Pulse parameters (normal vs power)
        double leadingEdgeW = isPower ? POWER_LEADING_EDGE_WIDTH : LEADING_EDGE_WIDTH;
        double wakeGlowW = isPower ? POWER_WAKE_GLOW_WIDTH : WAKE_GLOW_WIDTH;

        // Distance-based leading-edge glow (only during active sweep)
        float pulseMod = 1.0f;
        long elapsed = now - pulseStart;
        if (elapsed >= 0 && elapsed < pulseDurationMs) {
            // Reversed frontier: starts at totalLen, moves toward 0
            double frontierDist = totalLen * (1.0 - (double) elapsed / pulseDurationMs);
            double distToFrontier = Math.abs(dist - frontierDist);

            if (distToFrontier < leadingEdgeW) {
                // Leading edge: intense bright flash centered on the wavefront
                float intensity = 1.0f - (float) (distToFrontier / leadingEdgeW);
                intensity *= intensity; // Quadratic for sharp peak
                float glowMult = isPower ? 16.0f : 10.0f;
                float coreMult = isPower ? 10.0f : 6.0f;
                pulseMod = 1.0f + intensity * (isGlow ? glowMult : coreMult);
            } else if (dist > frontierDist) {
                // Wake glow: behind the pulse (already passed), gradually dimming
                double wakeDist = dist - frontierDist - leadingEdgeW;
                if (wakeDist > 0 && wakeDist < wakeGlowW) {
                    float wakeIntensity = 1.0f - (float) (wakeDist / wakeGlowW);
                    float wakeGlowMult = isPower ? 5.0f : 3.0f;
                    float wakeCoreMult = isPower ? 3.0f : 1.5f;
                    pulseMod = 1.0f + wakeIntensity * (isGlow ? wakeGlowMult : wakeCoreMult);
                }
            }
        }

        return Math.min(1.0f, baseAlpha * visibility * pulseMod);
    }

    /**
     * Computes the final alpha as the MAX of the current pulse and the
     * previous pulse. This prevents points from briefly blinking out when
     * a new pulse fires while the old trail is still fading.
     */
    private static float computeAlpha(double dist, double totalLen,
                                       long now, long pulseStart, long prevPulse, long pulseDurationMs,
                                       float baseAlpha, boolean isGlow,
                                       boolean isPower) {
        // End-of-trail fade
        float fade = 1.0f;
        if (dist < 4.0) fade = (float) (dist / 4.0);
        if (dist > totalLen - 4.0) fade = (float) ((totalLen - dist) / 4.0);
        fade = Math.max(0.0f, Math.min(1.0f, fade));

        float alphaCurrent = computeAlphaForPulse(dist, totalLen, now, pulseStart, pulseDurationMs, baseAlpha * fade, isGlow, isPower);
        float alphaPrev = computeAlphaForPulse(dist, totalLen, now, prevPulse, pulseDurationMs, baseAlpha * fade, isGlow, isPower);

        return Math.max(alphaCurrent, alphaPrev);
    }

    /**
     * Computes color whitening at the pulse leading edge.
     * Returns 0..0.7 (normal) or 0..0.9 (power) — the fraction to shift color toward white.
     */
    private static float computePulseWhiten(double dist, long now, long pulseStart,
                                             long pulseDurationMs, double totalLen, boolean isPower) {
        long elapsed = now - pulseStart;
        if (elapsed < 0 || elapsed >= pulseDurationMs) return 0;

        double leadingEdgeW = isPower ? POWER_LEADING_EDGE_WIDTH : LEADING_EDGE_WIDTH;

        // Reversed frontier: starts at totalLen, moves toward 0
        double frontierDist = totalLen * (1.0 - (double) elapsed / pulseDurationMs);
        double absDist = Math.abs(dist - frontierDist);
        if (absDist < leadingEdgeW) {
            float intensity = 1.0f - (float) (absDist / leadingEdgeW);
            float maxWhiten = isPower ? 0.9f : 0.7f;
            return intensity * intensity * maxWhiten;
        }
        return 0;
    }

    // ── Color resolution ────────────────────────────────────────────────

    private static float[] resolveColor() {
        String targetId = ActiveTrackingState.getTargetId() != null
                ? ActiveTrackingState.getTargetId().toString() : null;
        MenuCategory cat = ActiveTrackingState.getCategory();

        if (targetId != null) {
            var blockDef = BlockRegistry.getBlock(targetId);
            if (blockDef.isPresent()) {
                return blockDef.get().getColorAsFloats();
            }

            var blockTrigger = ScentTriggerConfigLoader.getBlockTrigger(targetId);
            if (blockTrigger.isPresent()) {
                var scent = ScentRegistry.getScentByName(blockTrigger.get().getScentName());
                if (scent.isPresent()) {
                    int[] rgb = scent.get().getColorRGB();
                    return new float[]{rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f};
                }
            }

            var biomeTrigger = ScentTriggerConfigLoader.getBiomeTrigger(targetId);
            if (biomeTrigger.isPresent()) {
                var scent = ScentRegistry.getScentByName(biomeTrigger.get().getScentName());
                if (scent.isPresent()) {
                    int[] rgb = scent.get().getColorRGB();
                    return new float[]{rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f};
                }
            }

            var structureTrigger = ScentTriggerConfigLoader.getStructureTrigger(targetId);
            if (structureTrigger.isPresent()) {
                var scent = ScentRegistry.getScentByName(structureTrigger.get().getScentName());
                if (scent.isPresent()) {
                    int[] rgb = scent.get().getColorRGB();
                    return new float[]{rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f};
                }
            }
        }

        if (cat == MenuCategory.STRUCTURES) {
            return new float[]{1.0f, 0.8f, 0.2f};
        }
        if (cat == MenuCategory.BIOMES) {
            return new float[]{0.3f, 0.9f, 0.5f};
        }

        return new float[]{0.8f, 0.85f, 1.0f};
    }

    // ── Client-side particle spawning ──────────────────────────────────

    private static void spawnPulseParticles(ClientLevel level, List<Vec3> points,
                                             double totalLen, long now, long pulseStart, long pulseDurationMs,
                                             float[] color, boolean isPower) {
        if (pulseStart <= 0 || points.size() < 2 || totalLen < 1.0) return;

        int argb = 0xFF000000
                | (Math.min(255, (int) (color[0] * 255)) << 16)
                | (Math.min(255, (int) (color[1] * 255)) << 8)
                | Math.min(255, (int) (color[2] * 255));
        var particle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, argb);

        double accum = 0.0;
        for (int i = 0; i < points.size(); i++) {
            Vec3 pt = points.get(i);
            if (i > 0) accum += points.get(i - 1).distanceTo(pt);

            int count = getParticleCount(accum, totalLen, now, pulseStart, pulseDurationMs, isPower);
            if (count <= 0) continue;
            if (count == 1 && i % 2 != 0) continue;

            for (int c = 0; c < count; c++) {
                level.addParticle(particle, pt.x, pt.y, pt.z, 0, 0, 0);
            }
        }
    }

    private static int getParticleCount(double dist, double totalLen, long now, long pulseStart, long pulseDurationMs,
                                         boolean isPower) {
        if (pulseStart <= 0) return 0;

        // Reversed: pulse starts at dest (dist=totalLen)
        double reachTime = pulseStart + ((totalLen - dist) / totalLen) * pulseDurationMs;
        double timeSincePassed = now - reachTime;

        if (timeSincePassed < 0) return 0;
        if (timeSincePassed > WAKE_VISIBLE_MS) return 0;

        double leadingEdgeW = isPower ? POWER_LEADING_EDGE_WIDTH : LEADING_EDGE_WIDTH;
        double wakeGlowW = isPower ? POWER_WAKE_GLOW_WIDTH : WAKE_GLOW_WIDTH;

        long elapsed = now - pulseStart;
        if (elapsed >= 0 && elapsed < pulseDurationMs) {
            // Reversed frontier
            double frontierDist = totalLen * (1.0 - (double) elapsed / pulseDurationMs);
            double distToFrontier = Math.abs(dist - frontierDist);

            if (distToFrontier < leadingEdgeW) return isPower ? 5 : 3;
            if (dist > frontierDist && (dist - frontierDist) < wakeGlowW) return isPower ? 3 : 2;
        }

        return 1;
    }

    // ── Player-tracking adjustment ──────────────────────────────────────

    /** Number of points to blend when adjusting the trail start to the player. */
    private static final int PLAYER_BLEND_POINTS = 10;

    /**
     * Returns a copy of the path with the first few points shifted so that
     * point[0] matches the current player position. Uses quadratic falloff
     * so the adjustment blends smoothly back to the original computed path.
     * The original list is NEVER mutated.
     */
    private static List<Vec3> adjustedForPlayer(List<Vec3> path, Vec3 originalPlayerPos, Vec3 currentPlayerPos) {
        if (path == null || path.size() < 2 || originalPlayerPos == null) return path;
        Vec3 delta = currentPlayerPos.subtract(originalPlayerPos);
        if (delta.lengthSqr() < 0.001) return path; // No significant movement

        List<Vec3> adjusted = new ArrayList<>(path);
        int blendCount = Math.min(PLAYER_BLEND_POINTS, adjusted.size());
        for (int i = 0; i < blendCount; i++) {
            double weight = 1.0 - (double) i / blendCount;
            weight *= weight; // Quadratic for smooth blend
            adjusted.set(i, path.get(i).add(delta.scale(weight)));
        }
        return adjusted;
    }

    private static long resolvePulseDurationMs(double pathLen) {
        int trackedDistance = ActiveTrackingState.getDistance();
        double basis = trackedDistance >= 0 ? trackedDistance : pathLen;
        double normalized = Math.max(0.0, Math.min(1.0, basis / MAX_RENDER_DISTANCE));
        // Quadratic easing: near target drops duration aggressively.
        double eased = normalized * normalized;
        long duration = (long) (PULSE_DURATION_NEAR_MS + (PULSE_DURATION_FAR_MS - PULSE_DURATION_NEAR_MS) * eased);
        return Math.max(PULSE_DURATION_NEAR_MS, Math.min(PULSE_DURATION_FAR_MS, duration));
    }

    private static long resolvePulseCycleMs(long pulseDurationMs) {
        long base = pulseDurationMs + 900L;
        return Math.max(PULSE_CYCLE_NEAR_MS, Math.min(PULSE_CYCLE_FAR_MS, base));
    }

    // ── Utility ─────────────────────────────────────────────────────────

    private static double totalLength(List<Vec3> points) {
        double len = 0;
        for (int i = 1; i < points.size(); i++) {
            len += points.get(i - 1).distanceTo(points.get(i));
        }
        return len;
    }

    // ── Inner RenderType factory ────────────────────────────────────────

    private abstract static class TrailRenderType extends RenderType {
        private TrailRenderType() {
            super("dummy", 0, false, false, () -> {}, () -> {});
        }

        static RenderType create(String name, double lineWidth) {
            return RenderType.create(
                    name,
                    1536,
                    TRAIL_PIPELINE,
                    CompositeState.builder()
                            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(lineWidth)))
                            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                            .setOutputState(ITEM_ENTITY_TARGET)
                            .createCompositeState(false)
            );
        }
    }
}
