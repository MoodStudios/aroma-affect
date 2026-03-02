package com.ovrtechnology.tutorial.regenarea;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a tutorial regeneration area where broken blocks automatically respawn.
 * <p>
 * Regen areas define a cuboid region (corner1 to corner2) where any blocks broken
 * by players will automatically regenerate after a configurable delay.
 * <p>
 * This is useful for tutorial mining sections where players need to practice
 * breaking blocks without permanently destroying the tutorial environment.
 */
public class TutorialRegenArea {

    private final String id;
    @Nullable
    private BlockPos corner1;
    @Nullable
    private BlockPos corner2;
    private int regenDelayTicks;
    private boolean enabled;
    private Map<BlockPos, String> savedBlocks = new HashMap<>();

    /**
     * Default regeneration delay: 5 seconds (100 ticks).
     */
    public static final int DEFAULT_REGEN_DELAY_TICKS = 100;

    public TutorialRegenArea(String id) {
        this.id = id;
        this.regenDelayTicks = DEFAULT_REGEN_DELAY_TICKS;
        this.enabled = true;
    }

    public TutorialRegenArea(String id, @Nullable BlockPos corner1, @Nullable BlockPos corner2,
                              int regenDelayTicks, boolean enabled, Map<BlockPos, String> savedBlocks) {
        this.id = id;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.regenDelayTicks = regenDelayTicks;
        this.enabled = enabled;
        this.savedBlocks = savedBlocks != null ? new HashMap<>(savedBlocks) : new HashMap<>();
    }

    public String getId() {
        return id;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Corners
    // ─────────────────────────────────────────────────────────────────────────────

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

    /**
     * Checks if both corners are set, making the area ready.
     */
    public boolean isComplete() {
        return corner1 != null && corner2 != null;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Regen Delay
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Gets the regeneration delay in ticks.
     */
    public int getRegenDelayTicks() {
        return regenDelayTicks;
    }

    /**
     * Sets the regeneration delay in ticks.
     *
     * @param ticks delay in ticks (20 ticks = 1 second)
     */
    public void setRegenDelayTicks(int ticks) {
        this.regenDelayTicks = Math.max(1, ticks);
    }

    /**
     * Gets the regeneration delay in seconds.
     */
    public float getRegenDelaySeconds() {
        return regenDelayTicks / 20.0f;
    }

    /**
     * Sets the regeneration delay in seconds.
     *
     * @param seconds delay in seconds
     */
    public void setRegenDelaySeconds(float seconds) {
        this.regenDelayTicks = Math.max(1, (int) (seconds * 20));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Enabled State
    // ─────────────────────────────────────────────────────────────────────────────

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Saved Blocks (snapshot for restoration)
    // ─────────────────────────────────────────────────────────────────────────────

    public Map<BlockPos, String> getSavedBlocks() {
        return savedBlocks;
    }

    public void setSavedBlocks(Map<BlockPos, String> savedBlocks) {
        this.savedBlocks = savedBlocks != null ? new HashMap<>(savedBlocks) : new HashMap<>();
    }

    public void saveBlock(BlockPos pos, String blockState) {
        savedBlocks.put(pos, blockState);
    }

    @Nullable
    public String getSavedBlock(BlockPos pos) {
        return savedBlocks.get(pos);
    }

    public boolean hasSavedBlock(BlockPos pos) {
        return savedBlocks.containsKey(pos);
    }

    public void clearSavedBlocks() {
        this.savedBlocks.clear();
    }

    public boolean hasSavedBlocks() {
        return !savedBlocks.isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Area Checking
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Checks if a position is inside the defined area (cuboid).
     *
     * @param pos the position to check
     * @return true if inside the area, false otherwise
     */
    public boolean isInsideArea(BlockPos pos) {
        if (!isComplete()) {
            return false;
        }

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

    /**
     * Gets the total number of blocks in this area.
     */
    public int getBlockCount() {
        if (!isComplete()) {
            return 0;
        }

        int sizeX = Math.abs(corner2.getX() - corner1.getX()) + 1;
        int sizeY = Math.abs(corner2.getY() - corner1.getY()) + 1;
        int sizeZ = Math.abs(corner2.getZ() - corner1.getZ()) + 1;
        return sizeX * sizeY * sizeZ;
    }
}
