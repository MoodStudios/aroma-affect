package com.ovrtechnology.tutorial.scentzone.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.network.TutorialScentZoneNetworking.ZoneClientData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Renders scent zone outlines in the world when "Show zones" is enabled.
 */
public final class TutorialScentZoneRenderer {

    private TutorialScentZoneRenderer() {}

    /**
     * Called from the block outline render hook to draw zone outlines.
     */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPos) {
        if (!TutorialScentZoneNetworking.isShowZoneOverlays()) {
            return;
        }

        List<ZoneClientData> zones = TutorialScentZoneNetworking.getClientZones();
        if (zones.isEmpty()) {
            return;
        }

        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());

        for (ZoneClientData zone : zones) {
            float r, g, b, a;
            if (zone.enabled()) {
                r = 1.0f; g = 0.41f; b = 0.71f; a = 0.8f; // Pink
            } else {
                r = 0.5f; g = 0.5f; b = 0.5f; a = 0.4f; // Grey
            }

            double x1 = zone.x() - zone.radiusX() - cameraPos.x;
            double y1 = zone.y() - zone.radiusY() - cameraPos.y;
            double z1 = zone.z() - zone.radiusZ() - cameraPos.z;
            double x2 = zone.x() + zone.radiusX() + 1 - cameraPos.x;
            double y2 = zone.y() + zone.radiusY() + 1 - cameraPos.y;
            double z2 = zone.z() + zone.radiusZ() + 1 - cameraPos.z;

            ShapeRenderer.renderLineBox(
                    poseStack.last(), lineConsumer,
                    x1, y1, z1, x2, y2, z2,
                    r, g, b, a
            );
        }
    }
}
