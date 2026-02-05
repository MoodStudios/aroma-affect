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
 * the player toward the destination. As the wave sweeps forward, it BUILDS
 * the path behind it. The trail is ONLY visible through the pulse — each
 * section fades to invisible ~2.5s after the pulse passes, then reappears
 * when the next pulse sweeps through.
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

    /** Time for the pulse wave to sweep from player to destination. */
    private static final long PULSE_DURATION_MS = 2500;
    /** Total cycle time between pulse fires. */
    private static final long PULSE_CYCLE_MS = 4000;
    /** Breathing animation period. */
    private static final long BREATHE_PERIOD_MS = 3000;

    // ── Pulse visibility ────────────────────────────────────────────────

    /** How long each trail section stays visible after the pulse passes. */
    private static final long WAKE_VISIBLE_MS = 2500;
    /** How long the fade-out takes after the visible period ends. */
    private static final long WAKE_FADE_MS = 1000;

    // ── Pulse visual parameters ─────────────────────────────────────────

    /** Width (in blocks) of the bright leading-edge flash. */
    private static final double LEADING_EDGE_WIDTH = 6.0;
    /** Width (in blocks) of elevated brightness behind the leading edge. */
    private static final double WAKE_GLOW_WIDTH = 20.0;
    // ── Two-path state (stable + incoming revealed by pulse) ────────────

    private static List<Vec3> stablePath = new ArrayList<>();
    private static List<Vec3> incomingPath = null;
    private static List<Vec3> prevStablePath = null;
    private static BlockPos cachedDest = null;
    private static long lastPulseStart = 0;
    private static long prevPulseStart = 0;

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

    private PathTrailRenderer() {}

    // ── Public entry point ──────────────────────────────────────────────

    public static void renderTrail(PoseStack poseStack, Vec3 cameraPos, MultiBufferSource consumers) {
        if (!ActiveTrackingState.isActivelyTracking()) return;

        BlockPos dest = ActiveTrackingState.getDestination();
        if (dest == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 playerPos = mc.player.position();
        long now = System.currentTimeMillis();

        // ── Destination changed → reset everything ──
        if (!dest.equals(cachedDest)) {
            stablePath = computeTrailPoints(playerPos, dest, mc.level);
            cachedDest = dest;
            incomingPath = new ArrayList<>(stablePath);
            prevStablePath = null;
            prevPulseStart = 0;
            lastPulseStart = now;
        }

        // ── Manage pulse lifecycle ──
        long elapsed = now - lastPulseStart;
        boolean pulseActive = incomingPath != null && elapsed < PULSE_DURATION_MS;

        // Pulse just completed → adopt incoming as stable
        if (!pulseActive && incomingPath != null) {
            stablePath = incomingPath;
            incomingPath = null;
        }

        // Time for a new pulse → recompute path from current player position
        if (incomingPath == null && elapsed >= PULSE_CYCLE_MS) {
            prevStablePath = stablePath;
            incomingPath = computeTrailPoints(playerPos, dest, mc.level);
            prevPulseStart = lastPulseStart;
            lastPulseStart = now;
            elapsed = 0;
            pulseActive = true;
        }

        // ── Use whichever path is current (static in world, no per-frame adjustment) ──
        List<Vec3> renderPoints = (pulseActive && incomingPath != null && !incomingPath.isEmpty())
                ? incomingPath : stablePath;
        if (renderPoints.size() < 2) return;

        // ── Color & animation ──
        float[] color = resolveColor();
        float r = color[0], g = color[1], b = color[2];

        double totalLen = totalLength(renderPoints);
        float breathe = 0.85f + 0.15f * (float) Math.sin(
                (now % BREATHE_PERIOD_MS) / (double) BREATHE_PERIOD_MS * Math.PI * 2.0);

        PoseStack.Pose pose = poseStack.last();

        // ── Render previous path fading on its own geometry ──
        if (prevStablePath != null && prevStablePath.size() >= 2 && prevPulseStart > 0) {
            double prevTotalLen = totalLength(prevStablePath);
            long lastPointReach = prevPulseStart + PULSE_DURATION_MS;
            if (now - lastPointReach < WAKE_VISIBLE_MS + WAKE_FADE_MS) {
                renderLayer(pose, cameraPos, consumers.getBuffer(TRAIL_GLOW),
                        prevStablePath, r, g, b, 0.12f * breathe,
                        now, prevPulseStart, 0, prevTotalLen, true);
                renderLayer(pose, cameraPos, consumers.getBuffer(TRAIL_CORE),
                        prevStablePath, r, g, b, 0.55f * breathe,
                        now, prevPulseStart, 0, prevTotalLen, false);
            } else {
                prevStablePath = null;
            }
        }

        // ── Render current path (current pulse only) ──
        renderLayer(pose, cameraPos, consumers.getBuffer(TRAIL_GLOW),
                renderPoints, r, g, b, 0.12f * breathe,
                now, lastPulseStart, 0, totalLen, true);
        renderLayer(pose, cameraPos, consumers.getBuffer(TRAIL_CORE),
                renderPoints, r, g, b, 0.55f * breathe,
                now, lastPulseStart, 0, totalLen, false);

        // ── Client-side particle spawning (synchronized with pulse) ──
        if (now - lastParticleSpawnTime >= PARTICLE_SPAWN_INTERVAL_MS) {
            lastParticleSpawnTime = now;
            spawnPulseParticles(mc.level, renderPoints, totalLen, now, lastPulseStart, color);
            if (prevStablePath != null && prevStablePath.size() >= 2) {
                double prevTotalLen = totalLength(prevStablePath);
                spawnPulseParticles(mc.level, prevStablePath, prevTotalLen, now, prevPulseStart, color);
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
        lastParticleSpawnTime = 0;
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
                                     long prevPulse, double totalLen, boolean isGlow) {
        double accumulated = 0.0;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i);
            Vec3 next = points.get(i + 1);
            double segLen = a.distanceTo(next);

            float alphaA = computeAlpha(accumulated, totalLen, now, pulseStart, prevPulse, baseAlpha, isGlow);
            float alphaB = computeAlpha(accumulated + segLen, totalLen, now, pulseStart, prevPulse, baseAlpha, isGlow);

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
            float whitenA = computePulseWhiten(accumulated, now, pulseStart, totalLen);
            float whitenB = computePulseWhiten(accumulated + segLen, now, pulseStart, totalLen);
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
     * Returns 0 if the pulse hasn't reached this point yet,
     * or if the point has fully faded since the pulse passed.
     */
    private static float computeAlphaForPulse(double dist, double totalLen,
                                               long now, long pulseStart,
                                               float baseAlpha, boolean isGlow) {
        if (pulseStart <= 0) return 0;

        // When did the pulse reach this point?
        double reachTime = pulseStart + (dist / totalLen) * PULSE_DURATION_MS;
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

        // Distance-based leading-edge glow (only during active sweep)
        float pulseMod = 1.0f;
        long elapsed = now - pulseStart;
        if (elapsed >= 0 && elapsed < PULSE_DURATION_MS) {
            double frontierDist = ((double) elapsed / PULSE_DURATION_MS) * totalLen;
            double distToFrontier = Math.abs(dist - frontierDist);

            if (distToFrontier < LEADING_EDGE_WIDTH) {
                // Leading edge: intense bright flash centered on the wavefront
                float intensity = 1.0f - (float) (distToFrontier / LEADING_EDGE_WIDTH);
                intensity *= intensity; // Quadratic for sharp peak
                pulseMod = 1.0f + intensity * (isGlow ? 10.0f : 6.0f);
            } else if (dist < frontierDist) {
                // Wake glow: behind the pulse, gradually dimming
                double wakeDist = frontierDist - dist - LEADING_EDGE_WIDTH;
                if (wakeDist > 0 && wakeDist < WAKE_GLOW_WIDTH) {
                    float wakeIntensity = 1.0f - (float) (wakeDist / WAKE_GLOW_WIDTH);
                    pulseMod = 1.0f + wakeIntensity * (isGlow ? 3.0f : 1.5f);
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
                                       long now, long pulseStart, long prevPulse,
                                       float baseAlpha, boolean isGlow) {
        // End-of-trail fade
        float fade = 1.0f;
        if (dist < 4.0) fade = (float) (dist / 4.0);
        if (dist > totalLen - 4.0) fade = (float) ((totalLen - dist) / 4.0);
        fade = Math.max(0.0f, Math.min(1.0f, fade));

        float alphaCurrent = computeAlphaForPulse(dist, totalLen, now, pulseStart, baseAlpha * fade, isGlow);
        float alphaPrev = computeAlphaForPulse(dist, totalLen, now, prevPulse, baseAlpha * fade, isGlow);

        return Math.max(alphaCurrent, alphaPrev);
    }

    /**
     * Computes color whitening at the pulse leading edge.
     * Returns 0..0.7 — the fraction to shift color toward white.
     */
    private static float computePulseWhiten(double dist, long now, long pulseStart, double totalLen) {
        long elapsed = now - pulseStart;
        if (elapsed < 0 || elapsed >= PULSE_DURATION_MS) return 0;

        double frontierDist = ((double) elapsed / PULSE_DURATION_MS) * totalLen;
        double absDist = Math.abs(dist - frontierDist);
        if (absDist < LEADING_EDGE_WIDTH) {
            float intensity = 1.0f - (float) (absDist / LEADING_EDGE_WIDTH);
            return intensity * intensity * 0.7f;
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
                                             double totalLen, long now, long pulseStart,
                                             float[] color) {
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

            int count = getParticleCount(accum, totalLen, now, pulseStart);
            if (count <= 0) continue;
            if (count == 1 && i % 2 != 0) continue;

            for (int c = 0; c < count; c++) {
                level.addParticle(particle, pt.x, pt.y, pt.z, 0, 0, 0);
            }
        }
    }

    private static int getParticleCount(double dist, double totalLen, long now, long pulseStart) {
        if (pulseStart <= 0) return 0;

        double reachTime = pulseStart + (dist / totalLen) * PULSE_DURATION_MS;
        double timeSincePassed = now - reachTime;

        if (timeSincePassed < 0) return 0;
        if (timeSincePassed > WAKE_VISIBLE_MS) return 0;

        long elapsed = now - pulseStart;
        if (elapsed >= 0 && elapsed < PULSE_DURATION_MS) {
            double frontierDist = ((double) elapsed / PULSE_DURATION_MS) * totalLen;
            double distToFrontier = Math.abs(dist - frontierDist);

            if (distToFrontier < LEADING_EDGE_WIDTH) return 3;
            if (dist < frontierDist && (frontierDist - dist) < WAKE_GLOW_WIDTH) return 2;
        }

        return 1;
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
