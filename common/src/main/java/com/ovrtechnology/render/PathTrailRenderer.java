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
 * Uses a custom NO_DEPTH_TEST pipeline so the trail is always visible,
 * with a traveling pulse animation, color gradient, and glow effect.
 * Trail points are cached and anchored to world positions for stability.
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

    // ── Animation parameters ────────────────────────────────────────────

    private static final double PULSE_LENGTH = 30.0;
    private static final long PULSE_PERIOD_MS = 4000;
    private static final long BREATHE_PERIOD_MS = 3000;

    // ── Cache parameters ────────────────────────────────────────────────

    private static final double CACHE_RECOMPUTE_MOVE_THRESHOLD = 5.0;
    private static final long CACHE_MIN_INTERVAL_MS = 500;
    private static final long CACHE_FORCE_INTERVAL_MS = 3000;

    // ── Cache state ─────────────────────────────────────────────────────

    private static List<Vec3> cachedPoints = List.of();
    private static BlockPos cachedDest = null;
    private static Vec3 cachedComputeOrigin = null;
    private static long lastComputeTime = 0;

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

    /**
     * Renders the X-ray trail from the player toward the tracked destination.
     * Called each frame from the platform-specific world render hook.
     */
    public static void renderTrail(PoseStack poseStack, Vec3 cameraPos, MultiBufferSource consumers) {
        if (!ActiveTrackingState.isActivelyTracking()) return;

        BlockPos dest = ActiveTrackingState.getDestination();
        if (dest == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 playerPos = mc.player.position();

        // Update cached trail if needed
        updateCache(playerPos, dest, mc.level);

        // Prepare render points: trim behind player, prepend connector
        List<Vec3> points = prepareRenderPoints(playerPos);
        if (points.size() < 2) return;

        // Resolve trail color from scent/block definitions
        float[] color = resolveColor();
        float r = color[0], g = color[1], b = color[2];

        // Animation timing
        long now = System.currentTimeMillis();
        double totalLen = totalLength(points);
        double pulsePos = ((now % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS) * (totalLen + PULSE_LENGTH);
        float breathe = 0.85f + 0.15f * (float) Math.sin((now % BREATHE_PERIOD_MS) / (double) BREATHE_PERIOD_MS * Math.PI * 2.0);

        PoseStack.Pose pose = poseStack.last();

        // Glow layer (thick, faint) — drawn first for correct blend order
        renderLayer(pose, cameraPos, consumers.getBuffer(TRAIL_GLOW),
                points, r, g, b, 0.12f * breathe, pulsePos, totalLen, true);

        // Core layer (thin, bright)
        renderLayer(pose, cameraPos, consumers.getBuffer(TRAIL_CORE),
                points, r, g, b, 0.55f * breathe, pulsePos, totalLen, false);
    }

    // ── Cache management ────────────────────────────────────────────────

    private static void updateCache(Vec3 playerPos, BlockPos dest, ClientLevel level) {
        long now = System.currentTimeMillis();

        boolean destChanged = !dest.equals(cachedDest);
        boolean playerMoved = cachedComputeOrigin == null
                || playerPos.distanceTo(cachedComputeOrigin) > CACHE_RECOMPUTE_MOVE_THRESHOLD;
        boolean minIntervalPassed = (now - lastComputeTime) >= CACHE_MIN_INTERVAL_MS;
        boolean forceRefresh = (now - lastComputeTime) >= CACHE_FORCE_INTERVAL_MS;

        if (destChanged || (playerMoved && minIntervalPassed) || forceRefresh) {
            cachedPoints = computeTrailPoints(playerPos, dest, level);
            cachedDest = dest;
            cachedComputeOrigin = playerPos;
            lastComputeTime = now;
        }
    }

    /**
     * Clears the trail cache. Call when tracking stops.
     */
    public static void clearCache() {
        cachedPoints = List.of();
        cachedDest = null;
        cachedComputeOrigin = null;
        lastComputeTime = 0;
    }

    // ── Prepare render points (trim + connector) ────────────────────────

    private static List<Vec3> prepareRenderPoints(Vec3 playerPos) {
        if (cachedPoints.size() < 2) return cachedPoints;

        // Find nearest cached point to the player
        int nearestIdx = 0;
        double nearestDistSq = Double.MAX_VALUE;
        for (int i = 0; i < cachedPoints.size(); i++) {
            double dSq = cachedPoints.get(i).distanceToSqr(playerPos);
            if (dSq < nearestDistSq) {
                nearestDistSq = dSq;
                nearestIdx = i;
            }
        }

        // Trim all points behind the nearest (player already passed them)
        // Keep at least from nearestIdx onward
        int startIdx = Math.max(0, nearestIdx - 1);

        // Build result: prepend player position as smooth connector, then cached points ahead
        List<Vec3> result = new ArrayList<>(cachedPoints.size() - startIdx + 1);

        // Skip cached points too close to player (avoid z-fighting / visual clutter)
        Vec3 firstCached = cachedPoints.get(startIdx);
        double distToFirst = playerPos.distanceTo(firstCached);
        if (distToFirst > 1.0) {
            // Add player position as connector point
            result.add(new Vec3(playerPos.x, playerPos.y + HEIGHT_OFFSET, playerPos.z));
        }

        for (int i = startIdx; i < cachedPoints.size(); i++) {
            result.add(cachedPoints.get(i));
        }

        return result;
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
            // Follow terrain when above ground, dive underground when dest is below
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

    /**
     * Gets walkable ground height at (x, z) using client-side world data.
     * Scans downward from the heightmap to skip trees, fences, etc.
     */
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
                                     float baseAlpha, double pulsePos, double totalLen,
                                     boolean isGlow) {
        double accumulated = 0.0;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i);
            Vec3 next = points.get(i + 1);
            double segLen = a.distanceTo(next);

            float alphaA = computeAlpha(accumulated, totalLen, pulsePos, baseAlpha, isGlow);
            float alphaB = computeAlpha(accumulated + segLen, totalLen, pulsePos, baseAlpha, isGlow);

            // Skip nearly-invisible segments
            if (alphaA < 0.01f && alphaB < 0.01f) {
                accumulated += segLen;
                continue;
            }

            // Subtle gradient: color shifts slightly toward white near destination
            float gradA = (float) (accumulated / totalLen);
            float gradB = (float) ((accumulated + segLen) / totalLen);
            float rA = r + (1.0f - r) * gradA * 0.25f;
            float gA = g + (1.0f - g) * gradA * 0.25f;
            float bA = b + (1.0f - b) * gradA * 0.25f;
            float rB = r + (1.0f - r) * gradB * 0.25f;
            float gB = g + (1.0f - g) * gradB * 0.25f;
            float bB = b + (1.0f - b) * gradB * 0.25f;

            // Normal = segment direction (used for line screen-space expansion)
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

    /**
     * Computes per-vertex alpha with fade-at-ends and traveling pulse modulation.
     */
    private static float computeAlpha(double dist, double totalLen, double pulsePos,
                                       float baseAlpha, boolean isGlow) {
        // Fade at both ends of the trail
        float fade = 1.0f;
        if (dist < 4.0) fade = (float) (dist / 4.0);
        if (dist > totalLen - 4.0) fade = (float) ((totalLen - dist) / 4.0);
        fade = Math.max(0.0f, Math.min(1.0f, fade));

        // Traveling pulse: brightens a section moving along the trail
        double distToPulse = Math.abs(dist - pulsePos);
        float pulseMod = 1.0f;
        if (distToPulse < PULSE_LENGTH * 0.5) {
            float intensity = 1.0f - (float) (distToPulse / (PULSE_LENGTH * 0.5));
            pulseMod = 1.0f + intensity * (isGlow ? 3.0f : 2.0f);
        }

        return Math.min(1.0f, baseAlpha * fade * pulseMod);
    }

    // ── Color resolution ────────────────────────────────────────────────

    private static float[] resolveColor() {
        String targetId = ActiveTrackingState.getTargetId() != null
                ? ActiveTrackingState.getTargetId().toString() : null;
        MenuCategory cat = ActiveTrackingState.getCategory();

        if (targetId != null) {
            // Block registry color (for blocks with custom definitions)
            var blockDef = BlockRegistry.getBlock(targetId);
            if (blockDef.isPresent()) {
                return blockDef.get().getColorAsFloats();
            }

            // Scent trigger → scent color (works for flowers, blocks, biomes, structures)
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

        // Category-based fallback colors
        if (cat == MenuCategory.STRUCTURES) {
            return new float[]{1.0f, 0.8f, 0.2f};
        }
        if (cat == MenuCategory.BIOMES) {
            return new float[]{0.3f, 0.9f, 0.5f};
        }

        // Default: soft white-blue
        return new float[]{0.8f, 0.85f, 1.0f};
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
