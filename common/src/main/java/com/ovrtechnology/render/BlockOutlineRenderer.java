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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;



/**
 * Renders an X-ray wireframe outline at the destination block position
 * when tracking blocks or flowers within 32 blocks.
 */
public final class BlockOutlineRenderer {

    private static final RenderPipeline LINES_NO_DEPTH_PIPELINE = RenderPipeline.builder()
            .withLocation("aromaaffect/pipeline/lines_no_depth")
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

    private static final RenderType LINES_NO_DEPTH = XRayRenderType.createLinesNoDepth();

    private BlockOutlineRenderer() {}

    public static void renderOutline(PoseStack poseStack, Vec3 cameraPos, MultiBufferSource consumers) {
        if (!ActiveTrackingState.shouldShowOutline()) {
            return;
        }

        BlockPos dest = ActiveTrackingState.getDestination();
        if (dest == null) return;

        // Resolve color: blocks from BlockRegistry, flowers from scent trigger config
        float r = 1.0f, g = 1.0f, b = 1.0f;
        float[] resolved = resolveColor();
        if (resolved != null) {
            r = resolved[0];
            g = resolved[1];
            b = resolved[2];
        }

        // Alpha fade-in: 32 blocks = 0 alpha, 24 blocks = full alpha
        int distance = ActiveTrackingState.getDistance();
        float baseAlpha;
        if (distance <= 24) {
            baseAlpha = 1.0f;
        } else {
            baseAlpha = 1.0f - (distance - 24) / 8.0f;
        }

        // Pulse animation
        float time = (System.currentTimeMillis() % 2000) / 2000.0f;
        float pulse = 0.7f + 0.3f * (float) Math.sin(time * Math.PI * 2.0);
        float alpha = baseAlpha * pulse;
        if (alpha <= 0.0f) return;

        // Camera-relative coordinates
        double dx = dest.getX() - cameraPos.x;
        double dy = dest.getY() - cameraPos.y;
        double dz = dest.getZ() - cameraPos.z;

        VertexConsumer lineConsumer = consumers.getBuffer(LINES_NO_DEPTH);

        // Pack RGB into a single int color (RRGGBB)
        int color = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        ShapeRenderer.renderShape(
                poseStack, lineConsumer,
                Shapes.block(),
                dx, dy, dz,
                color, alpha
        );
    }

    private static float[] resolveColor() {
        String targetId = ActiveTrackingState.getTargetId() != null
                ? ActiveTrackingState.getTargetId().toString() : null;
        if (targetId == null) return null;

        // Try BlockRegistry first (for BLOCKS category)
        var blockDef = BlockRegistry.getBlock(targetId);
        if (blockDef.isPresent()) {
            return blockDef.get().getColorAsFloats();
        }

        // For flowers (and any block not in BlockRegistry), resolve via scent trigger
        var trigger = ScentTriggerConfigLoader.getBlockTrigger(targetId);
        if (trigger.isPresent()) {
            String scentName = trigger.get().getScentName();
            var scent = ScentRegistry.getScentByName(scentName);
            if (scent.isPresent()) {
                int[] rgb = scent.get().getColorRGB();
                return new float[]{rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f};
            }
        }

        // Fallback for structures: gold/amber color
        if (ActiveTrackingState.getCategory() == MenuCategory.STRUCTURES) {
            return new float[]{1.0f, 0.8f, 0.2f};
        }

        return null;
    }

    /**
     * Helper class to create custom RenderType with no-depth lines.
     */
    private static final class XRayRenderType {
        private XRayRenderType() {}

        static RenderType createLinesNoDepth() {
            RenderSetup setup = RenderSetup.builder(LINES_NO_DEPTH_PIPELINE)
                    .bufferSize(1536)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup();
            return RenderType.create("aromaaffect_lines_no_depth", setup);
        }
    }
}
