package com.ovrtechnology.variant;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.nose.NoseUnlock;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Locale;

@Getter
@Setter
@Accessors(chain = true)
public class NoseVariant {

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Animation {
        @SerializedName("frames")
        private int frames;

        @SerializedName("ticks_per_frame")
        private int ticksPerFrame;

        public int getTicksPerFrame() {
            return ticksPerFrame > 0 ? ticksPerFrame : 4;
        }

        public boolean isAnimated() {
            return frames > 1;
        }
    }


    @SerializedName("id")
    private String id;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("description")
    private String description;

    @SerializedName("translation_key")
    private String translationKey;

    @SerializedName("image")
    private String image;

    @SerializedName("entity_texture")
    private String entityTexture;

    @SerializedName("rarity")
    private String rarity;

    @SerializedName("durability")
    private int durability;

    @SerializedName("repair")
    private String repair;

    @SerializedName("tier")
    private int tier;

    @SerializedName("track_cost")
    private int trackCost;

    @SerializedName("unlock")
    private NoseUnlock unlock;

    @SerializedName("animation")
    private Animation animation;

    @SerializedName("guide_page")
    private JsonElement guidePage;

    public int getDurability() {
        return durability > 0 ? durability : 250;
    }

    public int getTier() {
        return tier > 0 ? tier : 1;
    }

    public int getTrackCost() {
        return trackCost > 0 ? trackCost : 10;
    }

    public NoseUnlock getUnlock() {
        return unlock != null ? unlock : new NoseUnlock();
    }

    public String getRarity() {
        return rarity != null && !rarity.isEmpty() ? rarity.toUpperCase(Locale.ROOT) : "COMMON";
    }

    public String getDisplayName() {
        if (displayName != null && !displayName.isEmpty()) return displayName;
        if (id != null) return id;
        return "Unknown Nose";
    }

    public boolean isValid() {
        return id != null && !id.isEmpty();
    }
}
