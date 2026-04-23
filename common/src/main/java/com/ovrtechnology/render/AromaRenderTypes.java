package com.ovrtechnology.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class AromaRenderTypes {

    private AromaRenderTypes() {}

    private static RenderPipeline linesNoDepthPipeline(String location) {
        return RenderPipeline.builder()
                .withLocation(location)
                .withVertexShader("core/rendertype_lines")
                .withFragmentShader("core/rendertype_lines")
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("Fog", UniformType.UNIFORM_BUFFER)
                .withUniform("Globals", UniformType.UNIFORM_BUFFER)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withVertexFormat(
                        DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES)
                .build();
    }

    private static final RenderPipeline TRAIL_PIPELINE =
            linesNoDepthPipeline("aromaaffect/pipeline/trail_no_depth");

    public static RenderType trail(String name, double lineWidth) {
        return Factory.lines(name, TRAIL_PIPELINE, OptionalDouble.of(lineWidth));
    }

    private static final RenderPipeline BLOCK_OUTLINE_PIPELINE =
            linesNoDepthPipeline("aromaaffect/pipeline/lines_no_depth");

    public static final RenderType BLOCK_OUTLINE_LINES =
            Factory.lines(
                    "aromaaffect_lines_no_depth", BLOCK_OUTLINE_PIPELINE, OptionalDouble.empty());

    private abstract static class Factory extends RenderType {
        private Factory() {
            super("dummy", 0, false, false, () -> {}, () -> {});
        }

        static RenderType lines(String name, RenderPipeline pipeline, OptionalDouble lineWidth) {
            return create(
                    name,
                    1536,
                    pipeline,
                    CompositeState.builder()
                            .setLineState(new RenderStateShard.LineStateShard(lineWidth))
                            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                            .setOutputState(ITEM_ENTITY_TARGET)
                            .createCompositeState(false));
        }
    }
}
