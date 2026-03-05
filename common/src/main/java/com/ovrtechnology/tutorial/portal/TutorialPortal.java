package com.ovrtechnology.tutorial.portal;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a tutorial portal (teleportation zone).
 * <p>
 * A portal is defined by:
 * <ul>
 *   <li>A source area (cuboid defined by two corners)</li>
 *   <li>A destination position (where players teleport to)</li>
 *   <li>Optional destination rotation (yaw/pitch)</li>
 * </ul>
 * <p>
 * When a player enters the source area, they are teleported to the destination.
 */
public class TutorialPortal {

    private final String id;

    // Source area (cuboid)
    @Nullable
    private BlockPos sourceCorner1;
    @Nullable
    private BlockPos sourceCorner2;

    // Destination
    @Nullable
    private BlockPos destination;
    private float destYaw = 0.0f;
    private float destPitch = 0.0f;

    // Custom delay in ticks before teleportation (0 = use default)
    private int delayTicks = 0;

    public TutorialPortal(String id) {
        this.id = id;
    }

    public TutorialPortal(String id,
                          @Nullable BlockPos sourceCorner1,
                          @Nullable BlockPos sourceCorner2,
                          @Nullable BlockPos destination,
                          float destYaw,
                          float destPitch) {
        this.id = id;
        this.sourceCorner1 = sourceCorner1;
        this.sourceCorner2 = sourceCorner2;
        this.destination = destination;
        this.destYaw = destYaw;
        this.destPitch = destPitch;
    }

    public String getId() {
        return id;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Source Area
    // ─────────────────────────────────────────────────────────────────────────────

    @Nullable
    public BlockPos getSourceCorner1() {
        return sourceCorner1;
    }

    public void setSourceCorner1(@Nullable BlockPos pos) {
        this.sourceCorner1 = pos;
    }

    @Nullable
    public BlockPos getSourceCorner2() {
        return sourceCorner2;
    }

    public void setSourceCorner2(@Nullable BlockPos pos) {
        this.sourceCorner2 = pos;
    }

    /**
     * Checks if the source area is fully defined.
     */
    public boolean hasSourceArea() {
        return sourceCorner1 != null && sourceCorner2 != null;
    }

    /**
     * Checks if a position is inside the source area.
     */
    public boolean isInsideSourceArea(BlockPos pos) {
        if (!hasSourceArea()) {
            return false;
        }

        int minX = Math.min(sourceCorner1.getX(), sourceCorner2.getX());
        int maxX = Math.max(sourceCorner1.getX(), sourceCorner2.getX());
        int minY = Math.min(sourceCorner1.getY(), sourceCorner2.getY());
        int maxY = Math.max(sourceCorner1.getY(), sourceCorner2.getY());
        int minZ = Math.min(sourceCorner1.getZ(), sourceCorner2.getZ());
        int maxZ = Math.max(sourceCorner1.getZ(), sourceCorner2.getZ());

        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Destination
    // ─────────────────────────────────────────────────────────────────────────────

    @Nullable
    public BlockPos getDestination() {
        return destination;
    }

    public void setDestination(@Nullable BlockPos pos) {
        this.destination = pos;
    }

    public float getDestYaw() {
        return destYaw;
    }

    public void setDestYaw(float yaw) {
        this.destYaw = yaw;
    }

    public float getDestPitch() {
        return destPitch;
    }

    public void setDestPitch(float pitch) {
        this.destPitch = pitch;
    }

    /**
     * Checks if the destination is set.
     */
    public boolean hasDestination() {
        return destination != null;
    }

    /**
     * Checks if the portal is complete (source area + destination).
     */
    public boolean isComplete() {
        return hasSourceArea() && hasDestination();
    }

    /**
     * Gets the custom delay in ticks. Returns 0 if using the default.
     */
    public int getDelayTicks() {
        return delayTicks;
    }

    /**
     * Sets a custom delay in ticks before teleportation.
     * @param ticks delay in ticks (0 = use default)
     */
    public void setDelayTicks(int ticks) {
        this.delayTicks = ticks;
    }

    /**
     * Checks if this portal has a custom delay.
     */
    public boolean hasCustomDelay() {
        return delayTicks > 0;
    }
}
