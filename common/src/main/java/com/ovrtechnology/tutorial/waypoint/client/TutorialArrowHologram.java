package com.ovrtechnology.tutorial.waypoint.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders a 3D holographic arrow using UTF-8 characters.
 * The arrow floats in world space and always faces the player (billboard effect).
 */
public final class TutorialArrowHologram {

    // Arrow made of UTF-8 block characters
    private static final String[] ARROW_LINES = {
            "  ██  ",
            "  ██  ",
            "  ██  ",
            "██████",
            " ████ ",
            "  ██  "
    };

    // OVR Purple color (ARGB)
    private static final int ARROW_COLOR = 0xFFA890F0;

    // Position settings
    private static final float BASE_HEIGHT = 2.5f;
    private static final float BOB_AMPLITUDE = 0.15f;
    private static final float BOB_SPEED = 2.0f;

    // Scale of the text
    private static final float TEXT_SCALE = 0.025f;

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
        if (!isVisible()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // Check distance
        double distance = mc.player.position().distanceTo(targetPosition);
        if (distance > 60) {
            return;
        }

        // Calculate bobbing
        double time = (System.currentTimeMillis() % 10000) / 1000.0;
        float bobOffset = (float) Math.sin(time * BOB_SPEED) * BOB_AMPLITUDE;

        // World position relative to camera
        double x = targetPosition.x - camX;
        double y = targetPosition.y + BASE_HEIGHT + bobOffset - camY;
        double z = targetPosition.z - camZ;

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        // Billboard effect - face the camera
        float yaw = mc.gameRenderer.getMainCamera().getYRot();
        float pitch = mc.gameRenderer.getMainCamera().getXRot();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));

        // Scale down and flip (text renders upside down by default in world space)
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        // Render each line of the arrow
        Font font = mc.font;
        float lineHeight = font.lineHeight + 1;
        float totalHeight = ARROW_LINES.length * lineHeight;

        for (int i = 0; i < ARROW_LINES.length; i++) {
            String line = ARROW_LINES[i];
            float textWidth = font.width(line);
            float xOffset = -textWidth / 2f;
            float yOffset = (i * lineHeight) - (totalHeight / 2f);

            // Draw with background for better visibility
            font.drawInBatch(
                    Component.literal(line),
                    xOffset, yOffset,
                    ARROW_COLOR,
                    false,  // no shadow
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.SEE_THROUGH,
                    0x40000000,  // semi-transparent background
                    LightTexture.FULL_BRIGHT
            );
        }

        poseStack.popPose();
    }
}
