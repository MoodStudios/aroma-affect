package com.ovrtechnology.flower;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

/**
 * Represents a trackable flower definition loaded from JSON.
 *
 * <p>Each flower definition maps a Minecraft flower block to a scent and provides
 * display properties and trigger configuration.</p>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class FlowerDefinition {

    private static final Pattern HTML_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");

    public static final String DEFAULT_COLOR = "#FF69B4";

    @SerializedName("block_id")
    private String blockId;

    @SerializedName("scent_id")
    private String scentId;

    @SerializedName("fallback_name")
    private String fallbackName;

    @SerializedName("color_html")
    private String colorHtml;

    @SerializedName("trigger_on")
    private String triggerOn = "PROXIMITY";

    @SerializedName("range")
    private int range = 3;

    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDIUM;

    @SerializedName("intensity")
    private Double intensity;

    public FlowerDefinition() {
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
            // Fall through to default
        }
        return 0xFF69B4;
    }

    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }
        if (blockId != null && !blockId.isEmpty()) {
            return formatBlockIdAsName(blockId);
        }
        return "Unknown Flower";
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
        FlowerDefinition that = (FlowerDefinition) o;
        return blockId != null && blockId.equals(that.blockId);
    }

    @Override
    public int hashCode() {
        return blockId != null ? blockId.hashCode() : 0;
    }
}
