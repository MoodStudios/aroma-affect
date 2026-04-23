package com.ovrtechnology.block;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.tracking.RequiredItem;
import com.ovrtechnology.trigger.ScentPriority;
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
public class BlockDefinition {

    private static final Pattern HTML_COLOR_PATTERN =
            Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");

    public static final String DEFAULT_COLOR = "#FFFFFF";

    @SerializedName("block_id")
    private String blockId;

    @SerializedName("color_html")
    private String colorHtml;

    @SerializedName("scent_id")
    private String scentId;

    @SerializedName("fallback_name")
    private String fallbackName;

    @SerializedName("trigger_on")
    private String triggerOn = "PROXIMITY";

    @SerializedName("range")
    private int range = 5;

    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDIUM;

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
        return 0xFFFFFF;
    }

    public float[] getColorAsFloats() {
        int rgb = getColorAsInt();
        return new float[] {
            ((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f
        };
    }

    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }

        if (blockId != null && !blockId.isEmpty()) {
            return formatBlockIdAsName(blockId);
        }
        return "Unknown Block";
    }

    public String getTranslationKey() {
        if (blockId == null || !blockId.contains(":")) {
            return "block.minecraft.unknown";
        }
        String[] parts = blockId.split(":", 2);
        return "block." + parts[0] + "." + parts[1];
    }

    public boolean isValid() {
        return blockId != null && !blockId.isEmpty();
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

    private static String formatBlockIdAsName(String blockId) {

        String path = blockId.contains(":") ? blockId.split(":", 2)[1] : blockId;

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
        BlockDefinition that = (BlockDefinition) o;
        return blockId != null && blockId.equals(that.blockId);
    }

    @Override
    public int hashCode() {
        return blockId != null ? blockId.hashCode() : 0;
    }
}
