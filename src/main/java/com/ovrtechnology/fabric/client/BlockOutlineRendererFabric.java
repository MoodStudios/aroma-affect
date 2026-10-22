package com.ovrtechnology.fabric.client;

import com.ovrtechnology.render.BlockOutlineRenderer;
import com.ovrtechnology.render.PathTrailRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;

public final class BlockOutlineRendererFabric {

    private BlockOutlineRendererFabric() {}

    public static void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(BlockOutlineRendererFabric::onAfterTranslucent);
    }

    private static void onAfterTranslucent(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx) {
        OrderedSubmitNodeCollector collector = ctx.submitNodeCollector();

        PathTrailRenderer.renderTrail(
                ctx.poseStack(),
                ctx.levelState().cameraRenderState.pos,
                collector
        );

        BlockOutlineRenderer.renderOutline(
                ctx.poseStack(),
                ctx.levelState().cameraRenderState.pos,
                collector
        );
    }
}
