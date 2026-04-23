package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MobTriggerDefinition {

    public static final int DEFAULT_RANGE = 3;

    @SerializedName("entity_type")
    private String entityType;

    @SerializedName("scent_name")
    private String scentName;

    @SerializedName("range")
    private int range = DEFAULT_RANGE;

    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDLOW;

    @SerializedName("intensity")
    private Double intensity;

    @SerializedName("_comment")
    private String comment;

    public double getIntensityOrDefault(double globalIntensity) {
        return intensity != null ? intensity : globalIntensity;
    }

    public boolean isValid() {
        return entityType != null
                && !entityType.isEmpty()
                && scentName != null
                && !scentName.isEmpty()
                && range > 0;
    }
}
