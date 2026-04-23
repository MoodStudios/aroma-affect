package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BlockTriggerDefinition {

    public static final int DEFAULT_RANGE = 5;

    @SerializedName("block_id")
    private String blockId;

    @SerializedName("scent_name")
    private String scentName;

    @SerializedName("trigger_on")
    private String triggerOn = "PROXIMITY";

    @SerializedName("range")
    private int range = DEFAULT_RANGE;

    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDIUM;

    @SerializedName("intensity")
    private Double intensity;

    @SerializedName("_comment")
    private String comment;

    public boolean isProximityTrigger() {
        return "PROXIMITY".equalsIgnoreCase(triggerOn);
    }

    public boolean isInteractTrigger() {
        return "INTERACT".equalsIgnoreCase(triggerOn);
    }

    public double getIntensityOrDefault(double globalIntensity) {
        return intensity != null ? intensity : globalIntensity;
    }

    public boolean isValid() {
        return blockId != null && !blockId.isEmpty() && scentName != null && !scentName.isEmpty();
    }
}
