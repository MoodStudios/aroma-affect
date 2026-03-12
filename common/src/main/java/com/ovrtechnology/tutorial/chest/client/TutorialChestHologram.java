package com.ovrtechnology.tutorial.chest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

/**
 * Renders a 3D exclamation icon (PNG texture) above tutorial chests.
 * Billboard effect, bobbing animation, always faces the player.
 */
public final class TutorialChestHologram {

    private static final ResourceLocation EXCLAMATION_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/ovr_exclamation.png");

    private static final float BASE_HEIGHT = 1.55f;
    private static final float BOB_AMPLITUDE = 0.1f;
    private static final float BOB_SPEED = 2.0f;

    // Exclamation is taller than wide (~1:2 ratio)
    private static final float ICON_WIDTH = 0.45f;
    private static final float ICON_HEIGHT = 0.8f;

    private static final double MAX_RENDER_DISTANCE_SQ = 50.0 * 50.0;

    private static final Set<BlockPos> positions = new HashSet<>();

    private TutorialChestHologram() {
    }

    public static void setPositions(Set<BlockPos> newPositions) {
        positions.clear();
        positions.addAll(newPositions);
    }

    public static void addPosition(BlockPos pos) {
        positions.add(pos);
    }

    public static void removePosition(BlockPos pos) {
        positions.remove(pos);
    }

    public static void clear() {
        positions.clear();
    }

    /**
     * Renders exclamation icons above all tracked chest positions.
     * Called during world rendering from platform-specific render events.
     */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                               double camX, double camY, double camZ) {
        if (positions.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        Vec3 playerPos = mc.player.position();
        double time = (System.currentTimeMillis() % 10000) / 1000.0;

        for (BlockPos chestPos : positions) {
            double cx = chestPos.getX() + 0.5;
            double cy = chestPos.getY();
            double cz = chestPos.getZ() + 0.5;

            double distSq = playerPos.distanceToSqr(cx, cy, cz);
            if (distSq > MAX_RENDER_DISTANCE_SQ) {
                continue;
            }

            float bobOffset = (float) Math.sin(time * BOB_SPEED + chestPos.hashCode() * 0.3) * BOB_AMPLITUDE;

            double x = cx - camX;
            double y = cy + BASE_HEIGHT + bobOffset - camY;
            double z = cz - camZ;

            poseStack.pushPose();
            poseStack.translate(x, y, z);

            // Billboard effect
            float yaw = mc.gameRenderer.getMainCamera().getYRot();
            float pitch = mc.gameRenderer.getMainCamera().getXRot();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));

            // Render textured quad
            float halfW = ICON_WIDTH / 2f;
            float halfH = ICON_HEIGHT / 2f;

            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(EXCLAMATION_TEXTURE));
            int light = LightTexture.FULL_BRIGHT;
            int overlay = 0;

            consumer.addVertex(matrix, -halfW, -halfH, 0f)
                    .setColor(255, 255, 255, 255)
                    .setUv(0f, 1f)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(0f, 0f, -1f);

            consumer.addVertex(matrix, halfW, -halfH, 0f)
                    .setColor(255, 255, 255, 255)
                    .setUv(1f, 1f)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(0f, 0f, -1f);

            consumer.addVertex(matrix, halfW, halfH, 0f)
                    .setColor(255, 255, 255, 255)
                    .setUv(1f, 0f)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(0f, 0f, -1f);

            consumer.addVertex(matrix, -halfW, halfH, 0f)
                    .setColor(255, 255, 255, 255)
                    .setUv(0f, 0f)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(0f, 0f, -1f);

            poseStack.popPose();
        }
    }
}
