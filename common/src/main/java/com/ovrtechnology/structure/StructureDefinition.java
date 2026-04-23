package com.ovrtechnology.structure;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.tracking.RequiredItem;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.util.Colors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public class StructureDefinition {

    private static final Pattern HTML_COLOR_PATTERN =
            Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");

    private static final Pattern RESOURCE_LOCATION_PATTERN =
            Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    public static final String DEFAULT_COLOR = "#808080";

    public static final String DEFAULT_IMAGE = "structure/unknown";

    @SerializedName("structure_id")
    private String structureId;

    @SerializedName("image")
    private String image;

    @SerializedName("icon_block")
    private String iconBlock;

    @SerializedName("fallback_name")
    private String fallbackName;

    @SerializedName("color_html")
    private String colorHtml;

    @SerializedName("scent_id")
    private String scentId;

    @SerializedName("blocks")
    private List<String> blocks;

    @SerializedName("mode")
    private String mode = "PROXIMITY";

    @SerializedName("range")
    private int range = 50;

    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDLOW;

    @SerializedName("intensity")
    private Double intensity;

    @Getter(AccessLevel.NONE)
    @SerializedName("track_cost")
    private Integer trackCost;

    @Getter(AccessLevel.NONE)
    @SerializedName("required_item")
    private RequiredItem requiredItem;

    public StructureDefinition() {
        this.blocks = new ArrayList<>();
    }

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
        return Colors.STRUCTURE_DEFAULT_RGB;
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

        if (structureId != null && !structureId.isEmpty()) {
            return formatStructureIdAsName(structureId);
        }
        return "Unknown Structure";
    }

    public List<String> getBlocks() {
        return blocks != null ? blocks : Collections.emptyList();
    }

    public boolean hasBlocks() {
        return blocks != null && !blocks.isEmpty();
    }

    public String getTranslationKey() {
        if (structureId == null || !structureId.contains(":")) {
            return "structure.aromaaffect.unknown";
        }
        String[] parts = structureId.split(":", 2);
        return "structure.aromaaffect." + parts[0] + "." + parts[1];
    }

    public String getNamespace() {
        if (structureId != null && structureId.contains(":")) {
            return structureId.split(":", 2)[0];
        }
        return "";
    }

    public String getPath() {
        if (structureId != null && structureId.contains(":")) {
            return structureId.split(":", 2)[1];
        }
        return structureId != null ? structureId : "";
    }

    public boolean isVanilla() {
        return "minecraft".equals(getNamespace());
    }

    public boolean isValid() {
        return structureId != null && !structureId.isEmpty();
    }

    public boolean hasValidStructureIdFormat() {
        return structureId != null && RESOURCE_LOCATION_PATTERN.matcher(structureId).matches();
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

    private static String formatStructureIdAsName(String structureId) {

        String path = structureId.contains(":") ? structureId.split(":", 2)[1] : structureId;

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
        StructureDefinition that = (StructureDefinition) o;
        return structureId != null && structureId.equals(that.structureId);
    }

    @Override
    public int hashCode() {
        return structureId != null ? structureId.hashCode() : 0;
    }
}
