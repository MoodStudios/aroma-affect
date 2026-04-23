package com.ovrtechnology.sniffernose;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class SnifferNoseDefinition {

    @SerializedName("id")
    private String id;

    @SerializedName("image")
    private String image;

    @SerializedName("model")
    private String model;

    @SerializedName("tier")
    private int tier = 1;

    @SerializedName("durability")
    private int durability = 100;

    @SerializedName("repair")
    private String repair;

    @SerializedName("enabled")
    private boolean enabled = true;

    public String getModel() {
        return model != null && !model.isEmpty() ? model : "minecraft:leather_helmet";
    }

    public String getImage() {
        return image != null && !image.isEmpty() ? image : "item/sniffer_nose_default";
    }

    public int getDurability() {
        return durability > 0 ? durability : 100;
    }

    public String getTranslationKey() {
        return "item.aromaaffect." + id;
    }

    public boolean isValid() {
        return id != null && !id.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnifferNoseDefinition that = (SnifferNoseDefinition) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
