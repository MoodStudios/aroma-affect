package com.ovrtechnology.biome;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.tracking.RequiredItem;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.util.Colors;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class BiomeDefinition {

    private static final Pattern HTML_COLOR_PATTERN =
            Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");

    private static final Pattern RESOURCE_LOCATION_PATTERN =
            Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    public static final String DEFAULT_COLOR = "#5AA000";

    public static final String DEFAULT_IMAGE = "biome/unknown";

    @SerializedName("biome_id")
    private String biomeId;

    @SerializedName("image")
    private String image;

    @SerializedName("block")
    private String block;

    @SerializedName("fallback_name")
    private String fallbackName;

    @SerializedName("color_html")
    private String colorHtml;

    @SerializedName("scent_id")
    private String scentId;

    @SerializedName("mode")
    private String mode = "AMBIENT";

    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.LOW;

    @SerializedName("intensity")
    private Double intensity;

    @Getter(AccessLevel.NONE)
    @SerializedName("track_cost")
    private Integer trackCost;

    @Getter(AccessLevel.NONE)
    @SerializedName("required_item")
    private RequiredItem requiredItem;

    public int getTrackCost() {
        return trackCost != null && trackCost > 0 ? trackCost : 10;
    }

    public RequiredItem getRequiredItem() {
        return requiredItem;
    }

    public String getColorHtml() {
        if (isValidHtmlColor(colorHtml)) {
            return colorHtml.toUpperCase();
        }
        return DEFAULT_COLOR;
    }

    public String getRawColorHtml() {
        return colorHtml;
    }

    public int getColorAsInt() {
        String color = getColorHtml();
        try {
            String hex = color.substring(1);

            if (hex.length() == 3) {
                char r = hex.charAt(0);
                char g = hex.charAt(1);
                char b = hex.charAt(2);
                hex = "" + r + r + g + g + b + b;
            }

            if (hex.length() >= 6) {
                return Integer.parseInt(hex.substring(0, 6), 16);
            }
        } catch (Exception e) {

        }
        return Colors.BIOME_DEFAULT_RGB;
    }

    public float[] getColorAsFloats() {
        int rgb = getColorAsInt();
        return new float[] {
            ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f
        };
    }

    public String getImage() {
        return (image != null && !image.isEmpty()) ? image : DEFAULT_IMAGE;
    }

    public String getRawImage() {
        return image;
    }

    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }

        if (biomeId != null && !biomeId.isEmpty()) {
            return formatBiomeIdAsName(biomeId);
        }
        return "Unknown Biome";
    }

    public String getTranslationKey() {
        if (biomeId == null || !biomeId.contains(":")) {
            return "biome.minecraft.unknown";
        }
        String[] parts = biomeId.split(":", 2);
        return "biome." + parts[0] + "." + parts[1];
    }

    public String getNamespace() {
        if (biomeId != null && biomeId.contains(":")) {
            return biomeId.split(":", 2)[0];
        }
        return "";
    }

    public String getPath() {
        if (biomeId != null && biomeId.contains(":")) {
            return biomeId.split(":", 2)[1];
        }
        return biomeId != null ? biomeId : "";
    }

    public boolean isVanilla() {
        return "minecraft".equals(getNamespace());
    }

    public boolean isValid() {
        return biomeId != null && !biomeId.isEmpty();
    }

    public boolean hasValidBiomeIdFormat() {
        return biomeId != null && RESOURCE_LOCATION_PATTERN.matcher(biomeId).matches();
    }

    public boolean hasScentId() {
        return scentId != null && !scentId.isEmpty();
    }

    public static boolean isValidHtmlColor(String color) {
        if (color == null || color.isEmpty()) {
            return false;
        }
        return HTML_COLOR_PATTERN.matcher(color).matches();
    }

    public static boolean isValidResourceLocation(String resourceLocation) {
        if (resourceLocation == null || resourceLocation.isEmpty()) {
            return false;
        }
        return RESOURCE_LOCATION_PATTERN.matcher(resourceLocation).matches();
    }

    private static String formatBiomeIdAsName(String biomeId) {

        String path = biomeId.contains(":") ? biomeId.split(":", 2)[1] : biomeId;

        String[] parts = path.split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                if (result.length() > 0) {
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
        BiomeDefinition that = (BiomeDefinition) o;
        return biomeId != null && biomeId.equals(that.biomeId);
    }

    @Override
    public int hashCode() {
        return biomeId != null ? biomeId.hashCode() : 0;
    }
}
