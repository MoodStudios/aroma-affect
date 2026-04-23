package com.ovrtechnology.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ovrtechnology.block.BlockRegistry;
import com.ovrtechnology.menu.ActiveTrackingState;
import com.ovrtechnology.menu.MenuCategory;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class BlockOutlineRenderer {

    private BlockOutlineRenderer() {}

    public static void renderOutline(
            PoseStack poseStack, Vec3 cameraPos, MultiBufferSource consumers) {
        if (!ActiveTrackingState.shouldShowOutline()) {
            return;
        }

        BlockPos dest = ActiveTrackingState.getDestination();
        if (dest == null) return;

        float r = 1.0f, g = 1.0f, b = 1.0f;
        float[] resolved = resolveColor();
        if (resolved != null) {
            r = resolved[0];
            g = resolved[1];
            b = resolved[2];
        }

        int distance = ActiveTrackingState.getDistance();
        float baseAlpha;
        if (distance <= 24) {
            baseAlpha = 1.0f;
        } else {
            baseAlpha = 1.0f - (distance - 24) / 8.0f;
        }

        float time = (System.currentTimeMillis() % 2000) / 2000.0f;
        float pulse = 0.7f + 0.3f * (float) Math.sin(time * Math.PI * 2.0);
        float alpha = baseAlpha * pulse;
        if (alpha <= 0.0f) return;

        double dx = dest.getX() - cameraPos.x;
        double dy = dest.getY() - cameraPos.y;
        double dz = dest.getZ() - cameraPos.z;

        VertexConsumer lineConsumer = consumers.getBuffer(AromaRenderTypes.BLOCK_OUTLINE_LINES);

        LevelRenderer.renderLineBox(
                poseStack, lineConsumer, dx, dy, dz, dx + 1.0, dy + 1.0, dz + 1.0, r, g, b, alpha);
    }

    private static float[] resolveColor() {
        String targetId =
                ActiveTrackingState.getTargetId() != null
                        ? ActiveTrackingState.getTargetId().toString()
                        : null;
        if (targetId == null) return null;

        var blockDef = BlockRegistry.getBlock(targetId);
        if (blockDef.isPresent()) {
            return blockDef.get().getColorAsFloats();
        }

        var trigger = ScentTriggerConfigLoader.getBlockTrigger(targetId);
        if (trigger.isPresent()) {
            String scentName = trigger.get().getScentName();
            var scent = ScentRegistry.getScentByName(scentName);
            if (scent.isPresent()) {
                int[] rgb = scent.get().getColorRGB();
                return new float[] {rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f};
            }
        }

        if (ActiveTrackingState.getCategory() == MenuCategory.STRUCTURES) {
            return new float[] {1.0f, 0.8f, 0.2f};
        }

        return null;
    }
}
