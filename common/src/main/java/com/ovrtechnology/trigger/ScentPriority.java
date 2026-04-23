package com.ovrtechnology.trigger;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

public enum ScentPriority {
    @SerializedName("HIGH")
    HIGH(4),

    @SerializedName("MEDIUM")
    MEDIUM(3),

    @SerializedName("MEDLOW")
    MEDLOW(2),

    @SerializedName("LOW")
    LOW(1);

    @Getter private final int value;

    ScentPriority(int value) {
        this.value = value;
    }

    public int comparePriority(ScentPriority other) {
        return Integer.compare(this.value, other.value);
    }
}
