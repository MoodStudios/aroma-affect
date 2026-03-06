package com.ovrtechnology.tutorial.popupzone;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a popup HUD zone in the tutorial.
 * <p>
 * When a player enters the zone, an informative popup text appears
 * at the top-left of the screen. It disappears when they leave.
 */
public class TutorialPopupZone {

    private final String id;
    private String text;

    @Nullable
    private BlockPos corner1;
    @Nullable
    private BlockPos corner2;

    public TutorialPopupZone(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public TutorialPopupZone(String id, String text,
                              @Nullable BlockPos corner1, @Nullable BlockPos corner2) {
        this.id = id;
        this.text = text;
        this.corner1 = corner1;
        this.corner2 = corner2;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Nullable
    public BlockPos getCorner1() {
        return corner1;
    }

    public void setCorner1(@Nullable BlockPos pos) {
        this.corner1 = pos;
    }

    @Nullable
    public BlockPos getCorner2() {
        return corner2;
    }

    public void setCorner2(@Nullable BlockPos pos) {
        this.corner2 = pos;
    }

    public boolean hasArea() {
        return corner1 != null && corner2 != null;
    }

    public boolean isComplete() {
        return hasArea() && text != null && !text.isEmpty();
    }

    public boolean isInsideArea(BlockPos pos) {
        if (!hasArea()) return false;
        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }
}
