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
        NeoForge.EVENT_BUS.addListener(
                RenderLevelStageEvent.class, BlockOutlineRendererNeoForge::onRenderStage);
    }

    private static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource =
                Minecraft.getInstance().renderBuffers().bufferSource();

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        PathTrailRenderer.renderTrail(
                event.getPoseStack(), event.getCamera().getPosition(), bufferSource);

        BlockOutlineRenderer.renderOutline(
                event.getPoseStack(), event.getCamera().getPosition(), bufferSource);

        bufferSource.endBatch();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
    }
}
