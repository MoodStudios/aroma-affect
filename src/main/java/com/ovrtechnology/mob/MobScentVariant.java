package com.ovrtechnology.mob;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

/**
 * An alternative scent a mob gives off while it is in a given state, such as a
 * wolf that is wet. See {@link EntityConditions} for the condition keys.
 */
@Getter
public class MobScentVariant {

    @SerializedName("conditions")
    private JsonObject conditions;

    @SerializedName("scent_id")
    private String scentId;

    @SerializedName("perception")
    private String perception;
}
