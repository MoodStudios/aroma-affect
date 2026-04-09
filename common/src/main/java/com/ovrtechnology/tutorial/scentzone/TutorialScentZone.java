package com.ovrtechnology.tutorial.scentzone;

import net.minecraft.core.BlockPos;

/**
 * A scent trigger zone for the tutorial system.
 * When a player enters the zone, a specific scent is triggered.
 */
public class TutorialScentZone {

    private final String id;
    private BlockPos position;
    private int radiusX;
    private int radiusY;
    private int radiusZ;
    private String scentName;
    private double intensity;
    private int cooldownSeconds;
    private boolean oneShot; // true = only triggers once per session, false = re-triggers with cooldown
    private boolean enabled;

    public TutorialScentZone(String id, BlockPos position, int radiusX, int radiusY, int radiusZ, String scentName) {
        this.id = id;
        this.position = position;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.radiusZ = radiusZ;
        this.scentName = scentName;
        this.intensity = 0.5;
        this.cooldownSeconds = 5;
        this.oneShot = false;
        this.enabled = true;
    }

    public TutorialScentZone(String id, BlockPos position, int radiusX, int radiusY, int radiusZ, String scentName,
                              double intensity, int cooldownSeconds, boolean oneShot, boolean enabled) {
        this.id = id;
        this.position = position;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.radiusZ = radiusZ;
        this.scentName = scentName;
        this.intensity = intensity;
        this.cooldownSeconds = cooldownSeconds;
        this.oneShot = oneShot;
        this.enabled = enabled;
    }

    public String getId() { return id; }

    public BlockPos getPosition() { return position; }
    public void setPosition(BlockPos position) { this.position = position; }

    public int getRadiusX() { return radiusX; }
    public void setRadiusX(int radiusX) { this.radiusX = Math.max(1, radiusX); }

    public int getRadiusY() { return radiusY; }
    public void setRadiusY(int radiusY) { this.radiusY = Math.max(1, radiusY); }

    public int getRadiusZ() { return radiusZ; }
    public void setRadiusZ(int radiusZ) { this.radiusZ = Math.max(1, radiusZ); }

    public String getScentName() { return scentName; }
    public void setScentName(String scentName) { this.scentName = scentName; }

    public double getIntensity() { return intensity; }
    public void setIntensity(double intensity) { this.intensity = Math.max(0.0, Math.min(1.0, intensity)); }

    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = Math.max(0, cooldownSeconds); }

    public boolean isOneShot() { return oneShot; }
    public void setOneShot(boolean oneShot) { this.oneShot = oneShot; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Checks if a position is inside this zone (cuboid).
     */
    public boolean isInside(BlockPos pos) {
        int dx = Math.abs(pos.getX() - position.getX());
        int dy = Math.abs(pos.getY() - position.getY());
        int dz = Math.abs(pos.getZ() - position.getZ());
        return dx <= radiusX && dy <= radiusY && dz <= radiusZ;
    }
}
