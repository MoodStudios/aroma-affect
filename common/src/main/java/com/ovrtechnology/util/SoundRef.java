package com.ovrtechnology.util;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

public class SoundRef {
    @SerializedName("id")
    @Getter
    private String id;

    @SerializedName("volume")
    private float volume = 0.25f;

    @Getter
    @SerializedName("pitch")
    private float pitch = 1.0f;

    public SoundRef() {
    }

    public SoundRef(String id, float volume, float pitch) {
        this.id = id;
        this.volume = volume;
    }

    public float getVolume() {
        return Math.clamp(volume, 0.0f, 2f);
    }
}
