package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ItemTriggerDefinition {

    public static final int DEFAULT_DURATION_TICKS = 200;

    public static final ScentPriority DEFAULT_PRIORITY = ScentPriority.HIGH;

    public static final long DEFAULT_COOLDOWN_MS = 5000;

    @SerializedName("item_id")
    private String itemId;

    @SerializedName("scent_name")
    private String scentName;

    @SerializedName("trigger_on")
    private String triggerOn = "USE";

    @SerializedName("duration_ticks")
    private int durationTicks = DEFAULT_DURATION_TICKS;

    @SerializedName("priority")
    private ScentPriority priority = DEFAULT_PRIORITY;

    @SerializedName("cooldown_ms")
    private Long cooldownMs;

    @SerializedName("intensity")
    private Double intensity;

    public long getCooldownMsOrDefault() {
        return cooldownMs != null ? cooldownMs : DEFAULT_COOLDOWN_MS;
    }

    public double getIntensityOrDefault(double globalIntensity) {
        return intensity != null ? intensity : globalIntensity;
    }

    public ScentPriority getPriorityOrDefault() {
        return priority != null ? priority : DEFAULT_PRIORITY;
    }

    public boolean isUseTriggered() {
        return "USE".equalsIgnoreCase(triggerOn);
    }

    public boolean isValid() {
        return itemId != null && !itemId.isEmpty() && scentName != null && !scentName.isEmpty();
    }
}
