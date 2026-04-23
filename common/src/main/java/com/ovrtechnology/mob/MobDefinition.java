package com.ovrtechnology.mob;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class MobDefinition {

    private static final Pattern HTML_COLOR_PATTERN =
            Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");

    public static final String DEFAULT_COLOR = "#FFFFFF";

    @SerializedName("entity_type")
    private String entityType;

    @SerializedName("scent_id")
    private String scentId;

    @SerializedName("fallback_name")
    private String fallbackName;

    @SerializedName("color_html")
    private String colorHtml;

    @SerializedName("range")
    private int range = 3;

    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDLOW;

    @SerializedName("intensity")
    private Double intensity;

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

    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }
        if (entityType != null && !entityType.isEmpty()) {
            return formatEntityTypeAsName(entityType);
        }
        return "Unknown Mob";
    }

    public String getTranslationKey() {
        if (entityType == null || !entityType.contains(":")) {
            return "entity.minecraft.unknown";
        }
        String[] parts = entityType.split(":", 2);
        return "entity." + parts[0] + "." + parts[1];
    }

    public boolean isValid() {
        return entityType != null && !entityType.isEmpty();
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

    private static String formatEntityTypeAsName(String entityType) {
        String path = entityType.contains(":") ? entityType.split(":", 2)[1] : entityType;
        String[] parts = path.split("_");
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
        MobDefinition that = (MobDefinition) o;
        return entityType != null && entityType.equals(that.entityType);
    }

    @Override
    public int hashCode() {
        return entityType != null ? entityType.hashCode() : 0;
    }
}
