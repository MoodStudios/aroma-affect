package com.ovrtechnology.fabric.client;

import com.ovrtechnology.render.BlockOutlineRenderer;
import com.ovrtechnology.render.PathTrailRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public final class BlockOutlineRendererFabric {

    private BlockOutlineRendererFabric() {}

    public static void init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(
                context -> {
                    PathTrailRenderer.renderTrail(
                            context.matrices(),
                            context.worldState().cameraRenderState.pos,
                            context.consumers());
                    BlockOutlineRenderer.renderOutline(
                            context.matrices(),
                            context.worldState().cameraRenderState.pos,
                            context.consumers());
                });
    }
}
