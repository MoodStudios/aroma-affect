package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.render.BlockOutlineRenderer;
import com.ovrtechnology.render.PathTrailRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class BlockOutlineRendererNeoForge {

    private BlockOutlineRendererNeoForge() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.AfterTranslucentBlocks.class,
                BlockOutlineRendererNeoForge::onAfterTranslucent);
    }

    private static void onAfterTranslucent(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        PathTrailRenderer.renderTrail(
                event.getPoseStack(),
                event.getLevelRenderState().cameraRenderState.pos,
                bufferSource
        );

        BlockOutlineRenderer.renderOutline(
                event.getPoseStack(),
                event.getLevelRenderState().cameraRenderState.pos,
                bufferSource
        );

        bufferSource.endLastBatch();
    }
}
