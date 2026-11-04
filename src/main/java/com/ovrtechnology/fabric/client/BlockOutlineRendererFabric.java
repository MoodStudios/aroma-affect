package com.ovrtechnology.fabric.client;

import com.ovrtechnology.render.BlockOutlineRenderer;
import com.ovrtechnology.render.PathTrailRenderer;
//? if >=26 {
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
//?} else {
/*import com.ovrtechnology.legacy.render.OrderedSubmitNodeCollector;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

public final class BlockOutlineRendererFabric {

    private BlockOutlineRendererFabric() {}

    //? if >=26 {
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
    //?} else {
    /*public static void init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(BlockOutlineRendererFabric::onBeforeDebugRender);
    }

    private static void onBeforeDebugRender(WorldRenderContext ctx) {
        MultiBufferSource buffers = ctx.consumers() != null
                ? ctx.consumers()
                : Minecraft.getInstance().renderBuffers().bufferSource();
        OrderedSubmitNodeCollector collector = new OrderedSubmitNodeCollector(buffers);

        PathTrailRenderer.renderTrail(ctx.matrixStack(), ctx.camera().getPosition(), collector);
        BlockOutlineRenderer.renderOutline(ctx.matrixStack(), ctx.camera().getPosition(), collector);

        collector.flush();
    }
    *///?}
}
