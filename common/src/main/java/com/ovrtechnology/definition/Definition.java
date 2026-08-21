package com.ovrtechnology.definition;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.util.SoundRef;
import lombok.Getter;

import java.util.regex.Pattern;

public abstract class Definition {
    /**
     * Pattern for validating HTML hex color format.
     * Matches #RGB, #RRGGBB, or #RRGGBBAA formats.
     */
    protected static final Pattern HTML_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");

    /**
     * The ID (e.g., "minecraft:diamond_ore").
     * This is the primary identifier and must be unique.
     */
    @SerializedName("id")
    @Getter
    protected String id;

    /**
     * HTML hex color for UI display (e.g., "#5DECF5").
     * Used for rendering block indicators, trails, and menu items.
     */
    @SerializedName("color")
    protected String colorHtml;

    /**
     * Optional fallback display name when localization is unavailable.
     * If not provided, the ID will be formatted as the name.
     */
    @SerializedName("fallback_name")
    protected String fallbackName;

    @SerializedName("perception")
    @Getter
    protected String perception;

    @SerializedName("sound")
    @Getter
    protected SoundRef sound;

    /**
     * Default constructor for GSON deserialization.
     */
    public Definition() {
    }

    /**
     * Constructor for programmatic creation.
     *
     * @param id Minecraft ID
     * @param colorHtml HTML hex color
     */
    public Definition(String id, String fallbackName, String colorHtml) {
        this.id = id;
        this.fallbackName = fallbackName;
        this.colorHtml = colorHtml;
    }

    /**
     * Get the color with validation and fallback.
     *
     * @return Valid HTML color, or default white if invalid
     */
    public String getColorHtml() {
        if (isValidHtmlColor(colorHtml)) {
            return colorHtml.toUpperCase();
        }
        return getDefaultColor();
    }

    /**
     * Get the raw color value without validation.
     *
     * @return The raw color value as stored
     */
    public String getRawColorHtml() {
        return colorHtml;
    }

    /**
     * Parse the HTML color to an integer RGB value.
     *
     * @return RGB color as integer (0xRRGGBB format)
     */
    public int getColorAsInt() {
        String color = getColorHtml();
        try {
            // Remove the # prefix
            String hex = color.substring(1);

            // Handle short format (#RGB -> #RRGGBB)
            if (hex.length() == 3) {
                char r = hex.charAt(0);
                char g = hex.charAt(1);
                char b = hex.charAt(2);
                hex = "" + r + r + g + g + b + b;
            }

            // Parse only RGB portion (ignore alpha if present)
            if (hex.length() >= 6) {
                return Integer.parseInt(hex.substring(0, 6), 16);
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return 0xFFFFFF; // Default white
    }

    public int[] getColorRGB() {
        String color = getColorHtml();
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

    /**
     * Get the color components as float array [r, g, b] in range 0.0-1.0.
     *
     * @return Float array with RGB components
     */
    public float[] getColorAsFloats() {
        int rgb = getColorAsInt();
        return new float[] {
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f
        };
    }

    /**
     * Get the fallback name with sensible default.
     *
     * @return Fallback name, or ID if not set
     */
    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }
        // Auto-generate from ID (e.g., "minecraft:diamond_ore" -> "Diamond Ore")
        if (id != null && !id.isEmpty()) {
            return formatIDAsName(id);
        }
        return "Unknown " + getFallbackTitle();
    }

    /**
     * Get the localization key for this definition's display name.
     * Uses Minecraft's definition translation keys.
     *
     * @return Localization key in format "block.namespace.path"
     */
    public String getTranslationKey() {
        if (id == null || !id.contains(":")) {
            return getTranslationTitle() + ".aromaaffect.unknown";
        }
        String[] parts = id.split(":", 2);
        return getTranslationTitle() + "." + parts[0] + "." + parts[1];
    }

    /**
     * Validates the definition has required fields.
     *
     * @return true if the definition is valid
     */
    public boolean isValid() {
        return id != null && !id.isEmpty();
    }

    /**
     * Validate if a string is a valid HTML hex color.
     *
     * @param color The color string to validate
     * @return true if valid HTML hex color format
     */
    public static boolean isValidHtmlColor(String color) {
        if (color == null || color.isEmpty()) {
            return false;
        }
        return HTML_COLOR_PATTERN.matcher(color).matches();
    }

    /**
     * Format a ID as a display name.
     * Example: "minecraft:diamond_ore" -> "Diamond Ore"
     *
     * @param id The ID to format
     * @return Formatted display name
     */
    private static String formatIDAsName(String id) {
        // Remove namespace
        String path = id.contains(":") ? id.split(":", 2)[1] : id;

        // Convert underscores to spaces and capitalize
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
        Definition that = (Definition) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * Default color (white) when no valid color is specified.
     */
    protected String getDefaultColor() {
        return "#FFFFFF";
    }

    protected String getFallbackTitle() {
        return "";
    }

    protected String getTranslationTitle() {
        return "";
    }

    public static String getColor(Definition instance) {
        return instance.getDefaultColor();
    }
}
