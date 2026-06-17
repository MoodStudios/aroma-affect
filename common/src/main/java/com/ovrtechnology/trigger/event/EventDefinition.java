package com.ovrtechnology.trigger.event;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.ScentTriggerSource;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class EventDefinition {

    public enum DurationMode {
        @SerializedName("CONTINUOUS")
        CONTINUOUS,

        @SerializedName("ONE_SHOT")
        ONE_SHOT
    }

    private static final Pattern RESOURCE_LOCATION_PATTERN =
            Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    @SerializedName("event_id")
    private String eventId;

    @SerializedName("category")
    private String category;

    @SerializedName("trigger_type")
    private String triggerType;

    @SerializedName("scent_id")
    private String scentId;

    @SerializedName("intensity")
    private Double intensity;

    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDIUM;

    @SerializedName("duration_mode")
    private DurationMode durationMode = DurationMode.ONE_SHOT;

    @SerializedName("cooldown_ms")
    private Long cooldownMs;

    @SerializedName("yields_to_passive")
    private boolean yieldsToPassive = false;

    @Getter(AccessLevel.NONE)
    @SerializedName("conditions")
    private JsonObject conditions;

    public JsonObject getConditions() {
        return conditions != null ? conditions : new JsonObject();
    }

    public double getIntensity() {
        if (intensity == null) {
            return 0.5;
        }
        return Math.max(0.0, Math.min(1.0, intensity));
    }

    public long getCooldownMs() {
        return cooldownMs != null && cooldownMs >= 0 ? cooldownMs : 0L;
    }

    public int getDurationTicks() {
        return durationMode == DurationMode.CONTINUOUS ? -1 : 1;
    }

    public ScentTriggerSource resolveSource() {
        if (category == null) {
            return ScentTriggerSource.CUSTOM_EVENT;
        }
        try {
            return ScentTriggerSource.valueOf(category);
        } catch (IllegalArgumentException e) {
            return ScentTriggerSource.CUSTOM_EVENT;
        }
    }

    public boolean isContinuous() {
        return durationMode == DurationMode.CONTINUOUS;
    }

    public boolean isOneShot() {
        return durationMode == DurationMode.ONE_SHOT;
    }

    public boolean isValid() {
        return eventId != null
                && !eventId.isEmpty()
                && triggerType != null
                && !triggerType.isEmpty()
                && scentId != null
                && !scentId.isEmpty();
    }

    public boolean hasValidEventIdFormat() {
        return eventId != null && RESOURCE_LOCATION_PATTERN.matcher(eventId).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventDefinition that = (EventDefinition) o;
        return eventId != null && eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId != null ? eventId.hashCode() : 0;
    }
}
