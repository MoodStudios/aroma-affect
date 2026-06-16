package com.ovrtechnology.trigger.event;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;

/**
 * Data-driven definition of a single event/action scent hook, loaded from
 * {@code data/aromaaffect/scents/event_triggers.json}.
 *
 * <p>The detection logic for each hook lives in {@link EventScentHandlers}
 * (each event is bespoke). This definition only carries the tunable values —
 * which scent to play, how strong, how often, and at what priority — so a hook
 * can be retuned or disabled without touching code. The {@link #id} matches the
 * source id passed to {@link EventScentManager#fire}.</p>
 */
public final class EventTriggerDefinition {

    private String id;

    /** Exact OVR scent display name (e.g. "Petrichor", "Terra Silva"). */
    private String scent;

    private double intensity = 1.0;

    @SerializedName("cooldown_ms")
    private long cooldownMs = 5000;

    private String priority = "MEDIUM";

    @SerializedName("duration_ticks")
    private int durationTicks = 100;

    private boolean enabled = true;

    public String getId() {
        return id;
    }

    public String getScent() {
        return scent;
    }

    public double getIntensity() {
        return intensity;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Parses the configured priority string, defaulting to MEDIUM when missing
     * or invalid.
     */
    public ScentPriority getPriority() {
        if (priority == null) {
            return ScentPriority.MEDIUM;
        }
        try {
            return ScentPriority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ScentPriority.MEDIUM;
        }
    }

    public boolean isValid() {
        return id != null && !id.isEmpty() && scent != null && !scent.isEmpty();
    }
}
