package com.ovrtechnology.block;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

/**
 * Represents a trackable block definition loaded from JSON.
 * 
 * <p>Each block definition maps a Minecraft block to a scent and provides
 * a display color for UI rendering (e.g., in the radial menu or compass).</p>
 * 
 * <p>Example JSON entry:</p>
 * <pre>
 * {
 *   "block_id": "minecraft:diamond_ore",
 *   "color_html": "#5DECF5",
 *   "scent_id": "terra_silva"
 * }
 * </pre>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class BlockDefinition {
    
    /**
     * Pattern for validating HTML hex color format.
     * Matches #RGB, #RRGGBB, or #RRGGBBAA formats.
     */
    private static final Pattern HTML_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");
    
    /**
     * Default color (white) when no valid color is specified.
     */
    public static final String DEFAULT_COLOR = "#FFFFFF";
    
    /**
     * The Minecraft block ID (e.g., "minecraft:diamond_ore").
     * This is the primary identifier and must be unique.
     */
    @SerializedName("block_id")
    private String blockId;
    
    /**
     * HTML hex color for UI display (e.g., "#5DECF5").
     * Used for rendering block indicators, trails, and menu items.
     */
    @SerializedName("color_html")
    private String colorHtml;
    
    /**
     * Reference to a scent ID from scents.json.
     * This scent will be emitted when tracking this block.
     */
    @SerializedName("scent_id")
    private String scentId;
    
    /**
     * Optional fallback display name when localization is unavailable.
     * If not provided, the block ID will be formatted as the name.
     */
    @SerializedName("fallback_name")
    private String fallbackName;

    /**
     * Trigger mode: "INTERACT" (on right-click) or "PROXIMITY" (when near).
     */
    @SerializedName("trigger_on")
    private String triggerOn = "PROXIMITY";

    /**
     * Range in blocks for proximity triggers.
     */
    @SerializedName("range")
    private int range = 5;

    /**
     * Priority level for this trigger.
     */
    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.MEDIUM;

    /**
     * Scent intensity (0.0 to 1.0).
     * If not specified, uses the global block_intensity from settings.
     */
    @SerializedName("intensity")
    private Double intensity;

    /**
     * Default constructor for GSON deserialization.
     */
    public BlockDefinition() {
    }
    
    /**
     * Constructor for programmatic creation.
     * 
     * @param blockId Minecraft block ID
     * @param colorHtml HTML hex color
     * @param scentId Reference to scent definition
     */
    public BlockDefinition(String blockId, String colorHtml, String scentId) {
        this.blockId = blockId;
        this.colorHtml = colorHtml;
        this.scentId = scentId;
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
        return DEFAULT_COLOR;
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
     * @return Fallback name, or formatted block ID if not set
     */
    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }
        // Auto-generate from block ID (e.g., "minecraft:diamond_ore" -> "Diamond Ore")
        if (blockId != null && !blockId.isEmpty()) {
            return formatBlockIdAsName(blockId);
        }
        return "Unknown Block";
    }
    
    /**
     * Get the localization key for this block's display name.
     * Uses Minecraft's block translation keys.
     * 
     * @return Localization key in format "block.namespace.path"
     */
    public String getTranslationKey() {
        if (blockId == null || !blockId.contains(":")) {
            return "block.minecraft.unknown";
        }
        String[] parts = blockId.split(":", 2);
        return "block." + parts[0] + "." + parts[1];
    }
    
    /**
     * Validates the block definition has required fields.
     * 
     * @return true if the definition is valid
     */
    public boolean isValid() {
        return blockId != null && !blockId.isEmpty();
    }
    
    /**
     * Check if the scent_id field is set.
     * 
     * @return true if scent_id is specified
     */
    public boolean hasScentId() {
        return scentId != null && !scentId.isEmpty();
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
     * Format a block ID as a display name.
     * Example: "minecraft:diamond_ore" -> "Diamond Ore"
     * 
     * @param blockId The block ID to format
     * @return Formatted display name
     */
    private static String formatBlockIdAsName(String blockId) {
        // Remove namespace
        String path = blockId.contains(":") ? blockId.split(":", 2)[1] : blockId;
        
        // Convert underscores to spaces and capitalize
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

