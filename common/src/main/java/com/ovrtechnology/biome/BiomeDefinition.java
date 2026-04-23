package com.ovrtechnology.biome;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.tracking.RequiredItem;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

/**
 * Represents a trackable biome definition loaded from JSON.
 * 
 * <p>Each biome definition maps a Minecraft biome to a scent and provides
 * display properties for UI rendering (color, image, name).</p>
 * 
 * <p>Example JSON entry:</p>
 * <pre>
 * {
 *   "biome_id": "minecraft:jungle",
 *   "image": "biome/jungle",
 *   "fallback_name": "Jungle",
 *   "color_html": "#537B09",
 *   "scent_id": "evergreen"
 * }
 * </pre>
 * 
 * <h2>Biome ID Format</h2>
 * <p>Biome IDs follow the Minecraft ResourceLocation format: {@code namespace:path}.
 * For vanilla biomes, use {@code minecraft:} namespace. For modded biomes,
 * use the mod's namespace (e.g., {@code terralith:hot_shrubland}).</p>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class BiomeDefinition {
    
    /**
     * Pattern for validating HTML hex color format.
     * Matches #RGB, #RRGGBB, or #RRGGBBAA formats.
     */
    private static final Pattern HTML_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");
    
    /**
     * Pattern for validating ResourceLocation format (namespace:path).
     */
    private static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    
    /**
     * Default color (green) when no valid color is specified.
     */
    public static final String DEFAULT_COLOR = "#5AA000";
    
    /**
     * Default image path for fallback.
     */
    public static final String DEFAULT_IMAGE = "biome/unknown";
    
    /**
     * The Minecraft biome ID (e.g., "minecraft:jungle", "minecraft:desert").
     * This is the primary identifier and must be unique.
     * Must follow ResourceLocation format: namespace:path
     */
    @SerializedName("biome_id")
    private String biomeId;
    
    /**
     * Path to the texture file relative to assets/aromaaffect/textures/
     * Used for displaying biome icons in menus.
     */
    @SerializedName("image")
    private String image;

    @SerializedName("block")
    private String block;
    
    /**
     * Fallback display name when localization is unavailable.
     * If not provided, the biome ID will be formatted as the name.
     */
    @SerializedName("fallback_name")
    private String fallbackName;
    
    /**
     * HTML hex color for UI display (e.g., "#537B09").
     * Used for rendering biome indicators, trails, and menu items.
     * Often matches the biome's grass or foliage color.
     */
    @SerializedName("color_html")
    private String colorHtml;
    
    /**
     * Reference to a scent ID from scents.json.
     * This scent will be emitted when tracking or entering this biome.
     */
    @SerializedName("scent_id")
    private String scentId;

    /**
     * Trigger mode: "ENTER" (once on entry) or "AMBIENT" (continuous).
     */
    @SerializedName("mode")
    private String mode = "AMBIENT";

    /**
     * Priority level for this trigger.
     */
    @SerializedName("priority")
    private ScentPriority priority = ScentPriority.LOW;

    /**
     * Scent intensity (0.0 to 1.0).
     * If not specified, uses the global biome_intensity from settings.
     */
    @SerializedName("intensity")
    private Double intensity;

    @Getter(AccessLevel.NONE)
    @SerializedName("track_cost")
    private Integer trackCost;

    @Getter(AccessLevel.NONE)
    @SerializedName("required_item")
    private RequiredItem requiredItem;

    /**
     * Default constructor for GSON deserialization.
     */
    public BiomeDefinition() {
    }
    
    /**
     * Constructor for programmatic creation.
     * 
     * @param biomeId Minecraft biome ID
     * @param fallbackName Display name fallback
     * @param colorHtml HTML hex color
     * @param scentId Reference to scent definition
     */
    public BiomeDefinition(String biomeId, String fallbackName, String colorHtml, String scentId) {
        this.biomeId = biomeId;
        this.fallbackName = fallbackName;
        this.colorHtml = colorHtml;
        this.scentId = scentId;
    }
    
    public int getTrackCost() {
        return trackCost != null && trackCost > 0 ? trackCost : 10;
    }

    public RequiredItem getRequiredItem() {
        return requiredItem;
    }

    /**
     * Get the color with validation and fallback.
     *
     * @return Valid HTML color, or default green if invalid
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
        return 0x5AA000; // Default green
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
     * Get the image path with fallback.
     * 
     * @return Image path, or default if not set
     */
    public String getImage() {
        return (image != null && !image.isEmpty()) ? image : DEFAULT_IMAGE;
    }
    
    /**
     * Get the raw image value without fallback.
     * 
     * @return The raw image value as stored
     */
    public String getRawImage() {
        return image;
    }
    
    /**
     * Get the fallback name with sensible default.
     * 
     * @return Fallback name, or formatted biome ID if not set
     */
    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }
        // Auto-generate from biome ID
        if (biomeId != null && !biomeId.isEmpty()) {
            return formatBiomeIdAsName(biomeId);
        }
        return "Unknown Biome";
    }
    
    /**
     * Get the localization key for this biome's display name.
     * Uses Minecraft's biome translation key format.
     * 
     * @return Localization key in format "biome.namespace.path"
     */
    public String getTranslationKey() {
        if (biomeId == null || !biomeId.contains(":")) {
            return "biome.minecraft.unknown";
        }
        String[] parts = biomeId.split(":", 2);
        return "biome." + parts[0] + "." + parts[1];
    }
    
    /**
     * Get the namespace portion of the biome ID.
     * 
     * @return Namespace (e.g., "minecraft"), or empty string if invalid
     */
    public String getNamespace() {
        if (biomeId != null && biomeId.contains(":")) {
            return biomeId.split(":", 2)[0];
        }
        return "";
    }
    
    /**
     * Get the path portion of the biome ID.
     * 
     * @return Path (e.g., "jungle"), or the full ID if no namespace
     */
    public String getPath() {
        if (biomeId != null && biomeId.contains(":")) {
            return biomeId.split(":", 2)[1];
        }
        return biomeId != null ? biomeId : "";
    }
    
    /**
     * Check if this is a vanilla Minecraft biome.
     * 
     * @return true if namespace is "minecraft"
     */
    public boolean isVanilla() {
        return "minecraft".equals(getNamespace());
    }
    
    /**
     * Validates the biome definition has required fields.
     * 
     * @return true if the definition is valid
     */
    public boolean isValid() {
        return biomeId != null && !biomeId.isEmpty();
    }
    
    /**
     * Check if the biome_id has valid ResourceLocation format.
     * 
     * @return true if biome_id matches namespace:path format
     */
    public boolean hasValidBiomeIdFormat() {
        return biomeId != null && RESOURCE_LOCATION_PATTERN.matcher(biomeId).matches();
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
     * Validate if a string is a valid ResourceLocation format.
     * 
     * @param resourceLocation The resource location to validate
     * @return true if valid namespace:path format
     */
    public static boolean isValidResourceLocation(String resourceLocation) {
        if (resourceLocation == null || resourceLocation.isEmpty()) {
            return false;
        }
        return RESOURCE_LOCATION_PATTERN.matcher(resourceLocation).matches();
    }
    
    /**
     * Format a biome ID as a display name.
     * Example: "minecraft:jungle" -> "Jungle"
     * Example: "minecraft:snowy_plains" -> "Snowy Plains"
     * 
     * @param biomeId The biome ID to format
     * @return Formatted display name
     */
    private static String formatBiomeIdAsName(String biomeId) {
        // Remove namespace
        String path = biomeId.contains(":") ? biomeId.split(":", 2)[1] : biomeId;
        
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
        BiomeDefinition that = (BiomeDefinition) o;
        return biomeId != null && biomeId.equals(that.biomeId);
    }
    
    @Override
    public int hashCode() {
        return biomeId != null ? biomeId.hashCode() : 0;
    }
}

