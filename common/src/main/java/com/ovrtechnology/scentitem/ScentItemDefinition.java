package com.ovrtechnology.scentitem;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class ScentItemDefinition {

    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("scent")
    private String scent;

    @SerializedName("image")
    private String image;

    @SerializedName("model")
    private String model;

    @SerializedName("fallback_name")
    private String fallbackName;

    @SerializedName("description")
    private String description;

    @SerializedName("priority")
    private int priority = 5;

    @SerializedName("enabled")
    private boolean enabled = true;

    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }

        if (id != null && !id.isEmpty()) {
            return formatIdAsName(id);
        }
        return "Unknown Scent";
    }

    public String getModel() {
        return model != null && !model.isEmpty() ? model : "minecraft:paper";
    }

    public String getImage() {
        return image != null && !image.isEmpty() ? image : "item/scent_default";
    }

    public int getPriority() {
        return Math.max(1, Math.min(10, priority > 0 ? priority : 5));
    }

    public String getTranslationKey() {
        return "item.aromaaffect." + id;
    }

    public String getDescriptionTranslationKey() {
        return "scent.aromaaffect." + id + ".description";
    }

    public boolean isCapsule() {
        return "capsule".equals(type);
    }

    public boolean isValid() {
        return id != null && !id.isEmpty();
    }

    private static String formatIdAsName(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }

        String[] parts = id.split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                if (!result.isEmpty()) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1).toLowerCase());
                }
            }
        }

        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScentItemDefinition that = (ScentItemDefinition) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
