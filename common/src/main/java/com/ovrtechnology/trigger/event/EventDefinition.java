package com.ovrtechnology.trigger.event;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.definition.ImageDefinition;
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
public class EventDefinition extends ImageDefinition {

    public enum DurationMode {
        @SerializedName("CONTINUOUS")
        CONTINUOUS,

        @SerializedName("ONE_SHOT")
        ONE_SHOT
    }

    @SerializedName("category")
    private String category;

    @SerializedName("trigger_type")
    private String triggerType;

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
}
