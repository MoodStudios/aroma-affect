package com.ovrtechnology.tutorial.musiczone;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a music zone in the tutorial.
 * <p>
 * When a player enters the zone area, a sound starts playing.
 * When they leave, it stops.
 */
public class TutorialMusicZone {

    private final String id;
    private String soundId; // e.g., "aromaaffect:music.zone1"

    @Nullable
    private BlockPos corner1;
    @Nullable
    private BlockPos corner2;

    private float volume = 1.0f;
    private float pitch = 1.0f;

    public TutorialMusicZone(String id, String soundId) {
        this.id = id;
        this.soundId = soundId;
    }

    public TutorialMusicZone(String id, String soundId,
                              @Nullable BlockPos corner1, @Nullable BlockPos corner2,
                              float volume, float pitch) {
        this.id = id;
        this.soundId = soundId;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.volume = volume;
        this.pitch = pitch;
    }

    public String getId() {
        return id;
    }

    public String getSoundId() {
        return soundId;
    }

    public void setSoundId(String soundId) {
        this.soundId = soundId;
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

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public boolean isComplete() {
        return hasArea() && soundId != null && !soundId.isEmpty();
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
