package com.ovrtechnology.tutorial.searchdiamond.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a 3D holographic text "Do you want Diamonds?" at a world position.
 * The text floats in world space and always faces the player (billboard effect).
 * Uses the same rendering approach as TutorialArrowHologram.
 */
public final class DiamondTextHologram {

    // OVR Purple color
    private static final int TEXT_COLOR = 0xFFA890F0;

    // Position settings
    private static final float BASE_HEIGHT = 2.5f;
    private static final float BOB_AMPLITUDE = 0.15f;
    private static final float BOB_SPEED = 2.0f;

    // Text scale in world units
    private static final float TEXT_SCALE = 0.025f;

    // The hologram text
    private static final String HOLOGRAM_TEXT = "Do you want Diamonds?";

    private static Vec3 targetPosition = null;
    private static boolean visible = false;
    private static boolean initialized = false;

    private DiamondTextHologram() {
    }

    /**
     * Initializes the hologram renderer.
     * Note: This is called from AromaAffectClient but the actual rendering
     * is triggered from BlockOutlineRenderer on each platform.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        AromaAffect.LOGGER.debug("Diamond text hologram initialized");
    }

    /**
     * Sets the position where the text should render (in world coordinates).
     */
    public static void setTarget(Vec3 position) {
        targetPosition = position;
        visible = true;
        AromaAffect.LOGGER.info("DiamondTextHologram target set to: {}", position);
    }

    /**
     * Clears the text target.
     */
    public static void clear() {
        targetPosition = null;
        visible = false;
        AromaAffect.LOGGER.info("DiamondTextHologram cleared");
    }

    /**
     * Checks if the text is currently visible.
     */
    public static boolean isVisible() {
        return visible && targetPosition != null;
    }

    /**
     * Gets the current target position.
     */
    public static Vec3 getTarget() {
        return targetPosition;
    }

    /**
     * Renders the holographic text. Called during world rendering.
     *
     * @param poseStack    the pose stack for transformations
     * @param bufferSource the buffer source for rendering
     * @param camX         camera X position
     * @param camY         camera Y position
     * @param camZ         camera Z position
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

        // Scale down for world rendering
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        // Get font
        Font font = mc.font;
        int textWidth = font.width(HOLOGRAM_TEXT);

        // Center the text
        float textX = -textWidth / 2f;
        float textY = 0;

        // Render text with shadow
        font.drawInBatch(
                HOLOGRAM_TEXT,
                textX,
                textY,
                TEXT_COLOR,
                true, // shadow
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0, // background color (transparent)
                15728880 // full bright light
        );

        poseStack.popPose();
    }
}
