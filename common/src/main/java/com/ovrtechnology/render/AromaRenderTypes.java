package com.ovrtechnology.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class AromaRenderTypes {

    private AromaRenderTypes() {}

    public static RenderType trail(String name, double lineWidth) {
        return Factory.lines(name, OptionalDouble.of(lineWidth));
    }

    public static final RenderType BLOCK_OUTLINE_LINES =
            Factory.lines("aromaaffect_lines_no_depth", OptionalDouble.empty());

    private abstract static class Factory extends RenderType {
        private Factory() {
            super(
                    "dummy",
                    DefaultVertexFormat.POSITION_COLOR_NORMAL,
                    VertexFormat.Mode.LINES,
                    256,
                    false,
                    false,
                    () -> {},
                    () -> {});
        }

        static RenderType lines(String name, OptionalDouble lineWidth) {
            return create(
                    name,
                    DefaultVertexFormat.POSITION_COLOR_NORMAL,
                    VertexFormat.Mode.LINES,
                    1536,
                    false,
                    false,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_LINES_SHADER)
                            .setLineState(new RenderStateShard.LineStateShard(lineWidth))
                            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setOutputState(ITEM_ENTITY_TARGET)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .setCullState(NO_CULL)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .createCompositeState(false));
        }
    }
}
