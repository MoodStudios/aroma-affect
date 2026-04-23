package com.ovrtechnology.trigger;

import java.util.Objects;

public record ScentTrigger(
        String scentName,
        ScentTriggerSource source,
        ScentPriority priority,
        int durationTicks,
        double intensity,
        long triggeredAt) {

    public static final double DEFAULT_INTENSITY = 0.5;

    public static ScentTrigger create(
            String scentName,
            ScentTriggerSource source,
            ScentPriority priority,
            int durationTicks,
            double intensity) {
        return new ScentTrigger(
                Objects.requireNonNull(scentName, "scentName cannot be null"),
                Objects.requireNonNull(source, "source cannot be null"),
                Objects.requireNonNull(priority, "priority cannot be null"),
                durationTicks,
                Math.max(0.0, Math.min(1.0, intensity)),
                System.currentTimeMillis());
    }

    public static ScentTrigger create(
            String scentName,
            ScentTriggerSource source,
            ScentPriority priority,
            int durationTicks) {
        return create(scentName, source, priority, durationTicks, DEFAULT_INTENSITY);
    }

    public static ScentTrigger fromItemUse(String scentName, int durationTicks, double intensity) {
        return create(
                scentName,
                ScentTriggerSource.ITEM_USE,
                ScentPriority.HIGH,
                durationTicks,
                intensity);
    }

    public static ScentTrigger fromItemUse(String scentName, int durationTicks) {
        return fromItemUse(scentName, durationTicks, DEFAULT_INTENSITY);
    }

    public static ScentTrigger fromOmaraDevice(
            String scentName, int durationTicks, double intensity) {
        return create(
                scentName,
                ScentTriggerSource.OMARA_DEVICE,
                ScentPriority.HIGH,
                durationTicks,
                intensity);
    }

    public static ScentTrigger fromPassiveMode(
            String scentName, ScentPriority priority, int durationTicks, double intensity) {
        return create(
                scentName, ScentTriggerSource.PASSIVE_MODE, priority, durationTicks, intensity);
    }

    public boolean isIndefinite() {
        return durationTicks < 0;
    }

    public boolean shouldReplace(ScentTrigger other) {
        if (other == null) {
            return true;
        }

        int priorityComparison = this.priority.comparePriority(other.priority);

        if (priorityComparison > 0) {
            return true;
        }
        if (priorityComparison < 0) {
            return false;
        }

        return this.triggeredAt >= other.triggeredAt;
    }

    @Override
    public String toString() {
        return "ScentTrigger{"
                + "scentName='"
                + scentName
                + '\''
                + ", source="
                + source
                + ", priority="
                + priority
                + ", durationTicks="
                + durationTicks
                + ", intensity="
                + intensity
                + '}';
    }
}
