package com.ovrtechnology.tutorial.searchdiamond;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a diamond search zone for the tutorial.
 * Players must find a randomly placed diamond ore within the area.
 */
public class SearchDiamondZone {
    private final String id;
    @Nullable
    private BlockPos corner1;
    @Nullable
    private BlockPos corner2;
    @Nullable
    private BlockPos exitPoint;
    @Nullable
    private BlockPos triggerButton;
    @Nullable
    private BlockPos diamondLocation;

    public SearchDiamondZone(String id) {
        this.id = id;
    }

    public SearchDiamondZone(String id, @Nullable BlockPos corner1, @Nullable BlockPos corner2,
                              @Nullable BlockPos exitPoint, @Nullable BlockPos triggerButton) {
        this.id = id;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.exitPoint = exitPoint;
        this.triggerButton = triggerButton;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public BlockPos getCorner1() {
        return corner1;
    }

    public void setCorner1(@Nullable BlockPos corner1) {
        this.corner1 = corner1;
    }

    @Nullable
    public BlockPos getCorner2() {
        return corner2;
    }

    public void setCorner2(@Nullable BlockPos corner2) {
        this.corner2 = corner2;
    }

    @Nullable
    public BlockPos getExitPoint() {
        return exitPoint;
    }

    public void setExitPoint(@Nullable BlockPos exitPoint) {
        this.exitPoint = exitPoint;
    }

    @Nullable
    public BlockPos getTriggerButton() {
        return triggerButton;
    }

    public void setTriggerButton(@Nullable BlockPos triggerButton) {
        this.triggerButton = triggerButton;
    }

    @Nullable
    public BlockPos getDiamondLocation() {
        return diamondLocation;
    }

    public void setDiamondLocation(@Nullable BlockPos diamondLocation) {
        this.diamondLocation = diamondLocation;
    }

    public boolean hasArea() {
        return corner1 != null && corner2 != null;
    }

    public boolean isComplete() {
        return hasArea() && exitPoint != null;
    }

    public boolean isInsideArea(BlockPos pos) {
        if (!hasArea()) return false;
        int minX = Math.min(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public int getMinX() {
        return Math.min(corner1.getX(), corner2.getX());
    }

    public int getMinY() {
        return Math.min(corner1.getY(), corner2.getY());
    }

    public int getMinZ() {
        return Math.min(corner1.getZ(), corner2.getZ());
    }

    public int getMaxX() {
        return Math.max(corner1.getX(), corner2.getX());
    }

    public int getMaxY() {
        return Math.max(corner1.getY(), corner2.getY());
    }

    public int getMaxZ() {
        return Math.max(corner1.getZ(), corner2.getZ());
    }

    /**
     * Gets the center position of the zone for hologram display.
     */
    public BlockPos getCenter() {
        if (!hasArea()) return null;
        int centerX = (getMinX() + getMaxX()) / 2;
        int centerY = (getMinY() + getMaxY()) / 2;
        int centerZ = (getMinZ() + getMaxZ()) / 2;
        return new BlockPos(centerX, centerY, centerZ);
    }
}
