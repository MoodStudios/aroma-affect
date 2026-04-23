package com.ovrtechnology.fabric.client;

import com.ovrtechnology.render.BlockOutlineRenderer;
import com.ovrtechnology.render.PathTrailRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class BlockOutlineRendererFabric {

    private BlockOutlineRendererFabric() {}

    public static void init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(
                context -> {
                    PathTrailRenderer.renderTrail(
                            context.matrixStack(),
                            context.camera().getPosition(),
                            context.consumers());
                    BlockOutlineRenderer.renderOutline(
                            context.matrixStack(),
                            context.camera().getPosition(),
                            context.consumers());
                });
    }
}
