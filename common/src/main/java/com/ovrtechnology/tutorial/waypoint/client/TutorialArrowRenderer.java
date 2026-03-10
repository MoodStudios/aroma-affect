package com.ovrtechnology.tutorial.waypoint.client;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a floating arrow texture at the waypoint endpoint.
 * Uses screen-space projection to render the arrow as a HUD element
 * that appears at the world position.
 */
public final class TutorialArrowRenderer {

    // OVR Purple color
    private static final int ARROW_COLOR = 0xFFA890F0;
    private static final int ARROW_SHADOW_COLOR = 0xFF5040A0;

    // Bobbing animation parameters
    private static final float BOB_AMPLITUDE = 8.0f;  // Pixels up/down
    private static final float BOB_SPEED = 2.5f;      // Speed of bobbing

    // Height above the endpoint position (in blocks)
    private static final float BASE_HEIGHT = 2.2f;

    // Max render distance (blocks)
    private static final double MAX_RENDER_DISTANCE = 60.0;

    // Scale based on distance
    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 1.5f;

    private static boolean initialized = false;
    private static Vec3 targetPosition = null;
    private static boolean visible = false;

    private TutorialArrowRenderer() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Register HUD render event
        ClientGuiEvent.RENDER_HUD.register(TutorialArrowRenderer::onRenderHud);

        AromaAffect.LOGGER.debug("Tutorial arrow renderer initialized");
    }

    /**
     * Sets the position where the arrow should render (in world coordinates).
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
     * Checks if the arrow is currently visible.
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

    private static void onRenderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!isVisible()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) {
            return;
        }

        // Don't render if in GUI
        if (mc.screen != null) {
            return;
        }

        // Calculate world position with bobbing (in blocks)
        double time = (System.currentTimeMillis() % 10000) / 1000.0;
        float bobOffsetBlocks = (float) Math.sin(time * BOB_SPEED) * 0.15f;

        Vec3 worldPos = new Vec3(
                targetPosition.x,
                targetPosition.y + BASE_HEIGHT + bobOffsetBlocks,
                targetPosition.z
        );

        // Check distance
        double distance = mc.player.position().distanceTo(worldPos);
        if (distance > MAX_RENDER_DISTANCE) {
            return;
        }

        // Project world position to screen
        Vec3 screenPos = worldToScreen(worldPos, mc);
        if (screenPos == null) {
            return; // Behind camera or off-screen
        }

        // Calculate scale based on distance (closer = bigger)
        float distanceScale = (float) (1.0 - (distance / MAX_RENDER_DISTANCE));
        float scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * distanceScale;

        // Bobbing in screen space (pixels)
        float screenBob = (float) Math.sin(time * BOB_SPEED) * BOB_AMPLITUDE;

        // Center the arrow on the screen position
        int arrowWidth = (int) (8 * scale * 3.0f);  // Approximate width
        int x = (int) screenPos.x - arrowWidth / 2;
        int y = (int) screenPos.y + (int) screenBob;

        // Clamp to screen bounds with margin
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int margin = 10;

        if (x < margin) x = margin;
        if (y < margin) y = margin;
        if (x > screenWidth - arrowWidth - margin) x = screenWidth - arrowWidth - margin;
        if (y > screenHeight - arrowWidth - margin) y = screenHeight - arrowWidth - margin;

        // Render the arrow
        renderArrow(guiGraphics, x, y, scale);
    }

    private static Vec3 worldToScreen(Vec3 worldPos, Minecraft mc) {
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        // Position relative to camera
        float x = (float) (worldPos.x - cameraPos.x);
        float y = (float) (worldPos.y - cameraPos.y);
        float z = (float) (worldPos.z - cameraPos.z);

        // Camera angles
        float yaw = (float) Math.toRadians(camera.getYRot() + 180.0f);
        float pitch = (float) Math.toRadians(-camera.getXRot());

        // Rotate around Y (yaw)
        float cosYaw = (float) Math.cos(yaw);
        float sinYaw = (float) Math.sin(yaw);
        float rx = x * cosYaw + z * sinYaw;
        float rz = -x * sinYaw + z * cosYaw;

        // Rotate around X (pitch)
        float cosPitch = (float) Math.cos(pitch);
        float sinPitch = (float) Math.sin(pitch);
        float ry = y * cosPitch - rz * sinPitch;
        float rz2 = y * sinPitch + rz * cosPitch;

        // Behind camera check
        if (rz2 <= 0.1f) {
            return null;
        }

        // Get FOV and aspect ratio
        float fov = mc.options.fov().get().floatValue();
        float aspectRatio = (float) mc.getWindow().getWidth() / (float) mc.getWindow().getHeight();

        // Calculate projection scale
        float halfFovTan = (float) Math.tan(Math.toRadians(fov * 0.5));

        // Project to normalized device coordinates [-1, 1]
        float ndcX = rx / (rz2 * halfFovTan * aspectRatio);
        float ndcY = ry / (rz2 * halfFovTan);

        // Check if on screen (with some margin)
        if (ndcX < -1.0f || ndcX > 1.0f || ndcY < -1.0f || ndcY > 1.0f) {
            return null;
        }

        // Convert to GUI scaled screen coordinates
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();

        float screenX = (ndcX + 1.0f) * 0.5f * guiWidth;
        float screenY = (1.0f - ndcY) * 0.5f * guiHeight;

        return new Vec3(screenX, screenY, rz2);
    }

    private static void renderArrow(GuiGraphics guiGraphics, int x, int y, float scale) {
        int size = (int) (20 * scale);
        int halfSize = size / 2;
        int thickness = Math.max(2, (int) (4 * scale));

        // Draw arrow pointing down using rectangles
        // Shaft (vertical bar)
        int shaftWidth = thickness;
        int shaftHeight = size / 2;
        guiGraphics.fill(
                x + halfSize - shaftWidth / 2,
                y,
                x + halfSize + shaftWidth / 2,
                y + shaftHeight,
                ARROW_COLOR
        );

        // Arrow head (triangle made of horizontal lines)
        int headHeight = size / 2;
        int headWidth = size;
        for (int i = 0; i < headHeight; i++) {
            int lineWidth = (int) ((float) i / headHeight * headWidth);
            int lineX = x + halfSize - lineWidth / 2;
            guiGraphics.fill(
                    lineX,
                    y + shaftHeight + i,
                    lineX + lineWidth,
                    y + shaftHeight + i + 1,
                    ARROW_COLOR
            );
        }

        // Shadow/outline effect
        guiGraphics.fill(
                x + halfSize - shaftWidth / 2 - 1,
                y - 1,
                x + halfSize + shaftWidth / 2 + 1,
                y + shaftHeight,
                ARROW_SHADOW_COLOR
        );
    }
}
