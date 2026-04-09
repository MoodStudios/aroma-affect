package com.ovrtechnology.tutorial.waypoint.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders a 3D holographic arrow using a PNG texture.
 * The arrow floats in world space and always faces the player (billboard effect).
 */
public final class TutorialArrowHologram {

    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "textures/gui/tutorial/ovr_arrow.png");

    // Position settings
    private static final float BASE_HEIGHT = 1.8f;
    private static final float BOB_AMPLITUDE = 0.15f;
    private static final float BOB_SPEED = 2.0f;

    // Size of the rendered quad in world units
    private static final float ARROW_WIDTH = 1.0f;
    private static final float ARROW_HEIGHT = 1.2f;

    private static Vec3 targetPosition = null;
    private static boolean visible = false;

    private TutorialArrowHologram() {
    }

    /**
     * Sets the position where the arrow should render.
     */
    public static void setTarget(Vec3 position) {
        targetPosition = position;
        visible = true;
    }

    /**
     * Clears the arrow target.
     */
    public static void clear() {
        targetPosition = null;
        visible = false;
    }

    /**
     * Checks if the arrow is visible.
     */
    public static boolean isVisible() {
        return visible && targetPosition != null;
    }

    /**
     * Renders the holographic arrow. Called during world rendering.
     */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ) {
        // Render static arrows (always visible)
        for (var entry : TutorialStaticArrowManager.getClientArrows().entrySet()) {
            net.minecraft.core.BlockPos pos = entry.getValue();
            renderArrowAt(poseStack, bufferSource, camX, camY, camZ,
                    new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        }

        if (!isVisible()) {
            return;
        }

        renderArrowAt(poseStack, bufferSource, camX, camY, camZ, targetPosition);
    }

    private static void renderArrowAt(PoseStack poseStack, MultiBufferSource bufferSource,
                                       double camX, double camY, double camZ, Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double distance = mc.player.position().distanceTo(pos);
        if (distance > 60) return;

        double time = (System.currentTimeMillis() % 10000) / 1000.0;
        float bobOffset = (float) Math.sin(time * BOB_SPEED) * BOB_AMPLITUDE;

        double x = pos.x - camX;
        double y = pos.y + BASE_HEIGHT + bobOffset - camY;
        double z = pos.z - camZ;

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        float yaw = mc.gameRenderer.getMainCamera().getYRot();
        float pitch = mc.gameRenderer.getMainCamera().getXRot();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));

        float halfW = ARROW_WIDTH / 2f;
        float halfH = ARROW_HEIGHT / 2f;

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(ARROW_TEXTURE));
        int light = LightTexture.FULL_BRIGHT;

        consumer.addVertex(matrix, -halfW, -halfH, 0f).setColor(255, 255, 255, 255)
                .setUv(0f, 1f).setOverlay(0).setLight(light).setNormal(0f, 0f, -1f);
        consumer.addVertex(matrix, halfW, -halfH, 0f).setColor(255, 255, 255, 255)
                .setUv(1f, 1f).setOverlay(0).setLight(light).setNormal(0f, 0f, -1f);
        consumer.addVertex(matrix, halfW, halfH, 0f).setColor(255, 255, 255, 255)
                .setUv(1f, 0f).setOverlay(0).setLight(light).setNormal(0f, 0f, -1f);
        consumer.addVertex(matrix, -halfW, halfH, 0f).setColor(255, 255, 255, 255)
                .setUv(0f, 0f).setOverlay(0).setLight(light).setNormal(0f, 0f, -1f);

        poseStack.popPose();
    }
}
