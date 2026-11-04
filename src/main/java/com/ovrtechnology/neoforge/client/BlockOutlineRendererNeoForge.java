package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.render.BlockOutlineRenderer;
import com.ovrtechnology.render.PathTrailRenderer;
//? if >=26 {
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
//?} else {
/*import com.ovrtechnology.legacy.render.OrderedSubmitNodeCollector;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
*///?}
import net.neoforged.neoforge.common.NeoForge;

public final class BlockOutlineRendererNeoForge {

    private BlockOutlineRendererNeoForge() {}

    //? if >=26 {
    public static void init() {
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class,
                BlockOutlineRendererNeoForge::onSubmitCustomGeometry);
    }

    private static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        OrderedSubmitNodeCollector collector = event.getSubmitNodeCollector();

        PathTrailRenderer.renderTrail(
                event.getPoseStack(),
                event.getLevelRenderState().cameraRenderState.pos,
                collector
        );

        BlockOutlineRenderer.renderOutline(
                event.getPoseStack(),
                event.getLevelRenderState().cameraRenderState.pos,
                collector
        );
    }
    //?} else {
    /*public static void init() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, BlockOutlineRendererNeoForge::onRenderStage);
    }

    private static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        OrderedSubmitNodeCollector collector =
                new OrderedSubmitNodeCollector(Minecraft.getInstance().renderBuffers().bufferSource());

        PathTrailRenderer.renderTrail(event.getPoseStack(), event.getCamera().getPosition(), collector);
        BlockOutlineRenderer.renderOutline(event.getPoseStack(), event.getCamera().getPosition(), collector);

        collector.flush();
    }
    *///?}
}
