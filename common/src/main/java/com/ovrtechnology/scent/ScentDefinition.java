package com.ovrtechnology.scent;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class ScentDefinition {

    @SerializedName("id")
    private String id;

    @SerializedName("fallback_name")
    private String fallbackName;

    @SerializedName("description")
    private String description;

    @SerializedName("color")
    private String color;

    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }

        if (id != null && !id.isEmpty()) {
            return formatIdAsName(id);
        }
        return "Unknown Scent";
    }

    public String getTranslationKey() {
        return "scent.aromaaffect." + id;
    }

    public String getDescriptionTranslationKey() {
        return "scent.aromaaffect." + id + ".description";
    }

    public int[] getColorRGB() {
        if (color == null || color.isEmpty()) {
            return new int[] {255, 255, 255};
        }
        try {
            String hex = color.startsWith("#") ? color.substring(1) : color;
            int rgb = Integer.parseInt(hex, 16);
            return new int[] {(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
        } catch (NumberFormatException e) {
            return new int[] {255, 255, 255};
        }
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
        ScentDefinition that = (ScentDefinition) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
