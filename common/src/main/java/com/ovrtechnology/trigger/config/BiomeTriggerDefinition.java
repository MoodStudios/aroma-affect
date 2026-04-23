package com.ovrtechnology.trigger.config;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BiomeTriggerDefinition {

    @SerializedName("biome_id")
    private String biomeId;

    @SerializedName("scent_name")
    private String scentName;

    @SerializedName("mode")
    private String mode = "AMBIENT";

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
        return biomeId != null && !biomeId.isEmpty() && scentName != null && !scentName.isEmpty();
    }
}
