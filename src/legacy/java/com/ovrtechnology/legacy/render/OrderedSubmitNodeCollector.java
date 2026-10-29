package com.ovrtechnology.legacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

// Stand-in for the 26.x submit collector: draws straight into a 1.21.1 buffer source
public final class OrderedSubmitNodeCollector {

    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer consumer);
    }

    private final MultiBufferSource buffers;
    private final Set<RenderType> used = new LinkedHashSet<>();

    public OrderedSubmitNodeCollector(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer renderer) {
        used.add(renderType);
        renderer.render(poseStack.last(), buffers.getBuffer(renderType));
    }

    public void flush() {
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            for (RenderType type : used) {
                source.endBatch(type);
            }
        }
        used.clear();
    }
}
