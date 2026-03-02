package com.ovrtechnology.tutorial.boss;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a boss spawn area configuration.
 * <p>
 * Contains:
 * - Trigger area (cuboid where player must enter to spawn boss)
 * - Spawn position (where boss appears)
 * - Movement area (boss can't leave this area)
 * - Boss type (blaze, dragon)
 */
public class TutorialBossArea {

    private final String id;
    private final String bossType;  // "blaze" or "dragon"

    // Trigger area - when player enters, boss spawns
    @Nullable
    private BlockPos triggerMin;
    @Nullable
    private BlockPos triggerMax;

    // Where the boss spawns
    @Nullable
    private BlockPos spawnPos;

    // Movement bounds - boss can't leave this area
    @Nullable
    private BlockPos movementMin;
    @Nullable
    private BlockPos movementMax;

    public TutorialBossArea(String id, String bossType) {
        this.id = id;
        this.bossType = bossType;
    }

    public TutorialBossArea(String id, String bossType,
                            @Nullable BlockPos triggerMin, @Nullable BlockPos triggerMax,
                            @Nullable BlockPos spawnPos,
                            @Nullable BlockPos movementMin, @Nullable BlockPos movementMax) {
        this.id = id;
        this.bossType = bossType;
        this.triggerMin = triggerMin;
        this.triggerMax = triggerMax;
        this.spawnPos = spawnPos;
        this.movementMin = movementMin;
        this.movementMax = movementMax;
    }

    public String getId() {
        return id;
    }

    public String getBossType() {
        return bossType;
    }

    // Trigger area (corners - min/max calculated automatically)
    public void setTriggerCorner1(BlockPos pos) {
        this.triggerMin = pos;
    }

    public void setTriggerCorner2(BlockPos pos) {
        this.triggerMax = pos;
    }

    public boolean hasTriggerArea() {
        return triggerMin != null && triggerMax != null;
    }

    @Nullable
    public BlockPos getTriggerMin() {
        return triggerMin;
    }

    @Nullable
    public BlockPos getTriggerMax() {
        return triggerMax;
    }

    public boolean isInTriggerArea(BlockPos pos) {
        if (!hasTriggerArea()) return false;
        int minX = Math.min(triggerMin.getX(), triggerMax.getX());
        int maxX = Math.max(triggerMin.getX(), triggerMax.getX());
        int minY = Math.min(triggerMin.getY(), triggerMax.getY());
        int maxY = Math.max(triggerMin.getY(), triggerMax.getY());
        int minZ = Math.min(triggerMin.getZ(), triggerMax.getZ());
        int maxZ = Math.max(triggerMin.getZ(), triggerMax.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    // Spawn position
    public void setSpawnPos(BlockPos pos) {
        this.spawnPos = pos;
    }

    @Nullable
    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    public boolean hasSpawnPos() {
        return spawnPos != null;
    }

    // Movement area (corners - min/max calculated automatically)
    public void setMovementCorner1(BlockPos pos) {
        this.movementMin = pos;
    }

    public void setMovementCorner2(BlockPos pos) {
        this.movementMax = pos;
    }

    public boolean hasMovementArea() {
        return movementMin != null && movementMax != null;
    }

    @Nullable
    public BlockPos getMovementMin() {
        if (movementMin == null || movementMax == null) return movementMin;
        return new BlockPos(
                Math.min(movementMin.getX(), movementMax.getX()),
                Math.min(movementMin.getY(), movementMax.getY()),
                Math.min(movementMin.getZ(), movementMax.getZ())
        );
    }

    @Nullable
    public BlockPos getMovementMax() {
        if (movementMin == null || movementMax == null) return movementMax;
        return new BlockPos(
                Math.max(movementMin.getX(), movementMax.getX()),
                Math.max(movementMin.getY(), movementMax.getY()),
                Math.max(movementMin.getZ(), movementMax.getZ())
        );
    }

    // Raw corners for serialization
    @Nullable
    public BlockPos getMovementCorner1() {
        return movementMin;
    }

    @Nullable
    public BlockPos getMovementCorner2() {
        return movementMax;
    }

    public boolean isComplete() {
        return hasTriggerArea() && hasSpawnPos();
    }
}
