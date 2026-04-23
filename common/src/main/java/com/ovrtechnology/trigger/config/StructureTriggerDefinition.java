package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StructureTriggerDefinition {

    public static final int DEFAULT_RANGE = 50;

    @SerializedName("structure_id")
    private String structureId;

    @SerializedName("scent_name")
    private String scentName;

    @SerializedName("mode")
    private String mode = "PROXIMITY";

    @SerializedName("range")
    private int range = DEFAULT_RANGE;

    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDLOW;

    @SerializedName("intensity")
    private Double intensity;

    @SerializedName("_comment")
    private String comment;

    public boolean isProximityTrigger() {
        return "PROXIMITY".equalsIgnoreCase(mode);
    }

    public double getIntensityOrDefault(double globalIntensity) {
        return intensity != null ? intensity : globalIntensity;
    }

    public boolean isValid() {
        return structureId != null
                && !structureId.isEmpty()
                && scentName != null
                && !scentName.isEmpty();
    }
}
