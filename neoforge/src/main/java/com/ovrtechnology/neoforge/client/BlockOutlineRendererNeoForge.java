package com.ovrtechnology.neoforge.client;

import com.ovrtechnology.render.BlockOutlineRenderer;
import com.ovrtechnology.render.PathTrailRenderer;
import com.ovrtechnology.tutorial.waypoint.client.TutorialArrowHologram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
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
        Vec3 camPos = event.getLevelRenderState().cameraRenderState.pos;

        PathTrailRenderer.renderTrail(
                event.getPoseStack(),
                camPos,
                bufferSource
        );

        BlockOutlineRenderer.renderOutline(
                event.getPoseStack(),
                camPos,
                bufferSource
        );

        // Render waypoint arrow hologram
        TutorialArrowHologram.render(
                event.getPoseStack(),
                bufferSource,
                camPos.x, camPos.y, camPos.z
        );

        bufferSource.endLastBatch();
    }
}
