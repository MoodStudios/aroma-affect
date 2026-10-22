package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.render.BlockOutlineRenderer;
import com.ovrtechnology.render.PathTrailRenderer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class BlockOutlineRendererNeoForge {

    private BlockOutlineRendererNeoForge() {}

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
}
