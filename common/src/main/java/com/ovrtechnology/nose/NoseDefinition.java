package com.ovrtechnology.nose;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class NoseDefinition {

    @SerializedName("id")
    private String id;

    @SerializedName("image")
    private String image;

    @SerializedName("model")
    private String model;

    @SerializedName("unlock")
    private NoseUnlock unlock;

    @SerializedName("durability")
    private int durability;

    @SerializedName("repair")
    private String repair;

    @SerializedName("tier")
    private int tier;

    @SerializedName("track_cost")
    private int trackCost;

    @SerializedName("enabled")
    private boolean enabled = true;

    public String getModel() {
        return model != null && !model.isEmpty() ? model : "minecraft:iron_helmet";
    }

    public int getDurability() {
        return durability > 0 ? durability : 100;
    }

    public int getTrackCost() {
        return trackCost > 0 ? trackCost : 10;
    }

    public NoseUnlock getUnlock() {
        return unlock != null ? unlock : new NoseUnlock();
    }

    public boolean isValid() {
        return id != null && !id.isEmpty();
    }
}
