package com.ovrtechnology.tutorial.finishscreen;

import net.minecraft.core.BlockPos;

/**
 * Data class representing a finish screen trigger zone.
 */
public class TutorialFinishZone {

    private final String id;
    private BlockPos corner1;
    private BlockPos corner2;

    public TutorialFinishZone(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public BlockPos getCorner1() { return corner1; }
    public void setCorner1(BlockPos pos) { this.corner1 = pos; }

    public BlockPos getCorner2() { return corner2; }
    public void setCorner2(BlockPos pos) { this.corner2 = pos; }

    public boolean isComplete() {
        return corner1 != null && corner2 != null;
    }

    public boolean isInsideArea(BlockPos pos) {
        if (!isComplete()) return false;
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
}
