package com.ovrtechnology.fabric.client;

import com.ovrtechnology.render.BlockOutlineRenderer;
import com.ovrtechnology.render.PathTrailRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class BlockOutlineRendererFabric {

    private BlockOutlineRendererFabric() {}

    public static void init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(
                context -> {
                    com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
                    PathTrailRenderer.renderTrail(
                            context.matrixStack(),
                            context.camera().getPosition(),
                            context.consumers());
                    BlockOutlineRenderer.renderOutline(
                            context.matrixStack(),
                            context.camera().getPosition(),
                            context.consumers());

                    if (context.consumers()
                            instanceof
                            net.minecraft.client.renderer.MultiBufferSource.BufferSource
                            bs) {
                        bs.endBatch();
                    }
                    com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                    com.mojang.blaze3d.systems.RenderSystem.depthFunc(
                            org.lwjgl.opengl.GL11.GL_LEQUAL);
                });
    }
}
