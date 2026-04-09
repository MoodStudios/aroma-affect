package com.ovrtechnology.fabric.client;

import com.ovrtechnology.render.BlockOutlineRenderer;
import com.ovrtechnology.render.PathTrailRenderer;
import com.ovrtechnology.tutorial.chest.client.TutorialChestHologram;
import com.ovrtechnology.tutorial.scentzone.client.TutorialScentZoneRenderer;
import com.ovrtechnology.tutorial.searchdiamond.client.DiamondTextHologram;
import com.ovrtechnology.tutorial.waypoint.client.TutorialArrowHologram;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.world.phys.Vec3;

public final class BlockOutlineRendererFabric {

    private BlockOutlineRendererFabric() {}

    public static void init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            Vec3 camPos = context.worldState().cameraRenderState.pos;

            PathTrailRenderer.renderTrail(
                    context.matrices(),
                    camPos,
                    context.consumers()
            );
            BlockOutlineRenderer.renderOutline(
                    context.matrices(),
                    camPos,
                    context.consumers()
            );

            // Render waypoint arrow hologram
            TutorialArrowHologram.render(
                    context.matrices(),
                    context.consumers(),
                    camPos.x, camPos.y, camPos.z
            );

            // Render chest exclamation icons
            TutorialChestHologram.render(
                    context.matrices(),
                    context.consumers(),
                    camPos.x, camPos.y, camPos.z
            );

            // Render diamond text hologram
            DiamondTextHologram.render(
                    context.matrices(),
                    context.consumers(),
                    camPos.x, camPos.y, camPos.z
            );

            // Render scent zone outlines
            TutorialScentZoneRenderer.render(
                    context.matrices(),
                    context.consumers(),
                    camPos
            );
        });
    }
}
