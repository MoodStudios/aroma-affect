package com.ovrtechnology.structure;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a trackable structure definition loaded from JSON.
 * 
 * <p>Each structure definition maps a Minecraft structure to a scent and provides
 * display properties for UI rendering (color, image, name).</p>
 * 
 * <p>Structures can also specify a list of characteristic blocks that help
 * identify the structure type (e.g., end portal frames for strongholds).</p>
 * 
 * <p>Example JSON entry:</p>
 * <pre>
 * {
 *   "structure_id": "minecraft:stronghold",
 *   "image": "structure/stronghold",
 *   "fallback_name": "Stronghold",
 *   "color_html": "#4A4A4A",
 *   "scent_id": "terra_silva",
 *   "blocks": [
 *     "minecraft:end_portal_frame",
 *     "minecraft:stone_bricks"
 *   ]
 * }
 * </pre>
 * 
 * <h2>Structure ID Format</h2>
 * <p>Structure IDs follow the Minecraft ResourceLocation format: {@code namespace:path}.
 * For vanilla structures, use {@code minecraft:} namespace. For modded structures,
 * use the mod's namespace (e.g., {@code create:contraption_base}).</p>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class StructureDefinition {
    
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
     * Default color (gray) when no valid color is specified.
     */
    public static final String DEFAULT_COLOR = "#808080";
    
    /**
     * Default image path for fallback.
     */
    public static final String DEFAULT_IMAGE = "structure/unknown";
    
    /**
     * The Minecraft structure ID (e.g., "minecraft:stronghold", "minecraft:village_plains").
     * This is the primary identifier and must be unique.
     * Must follow ResourceLocation format: namespace:path
     */
    @SerializedName("structure_id")
    private String structureId;
    
    /**
     * Path to the texture file relative to assets/aromaaffect/textures/
     * Used for displaying structure icons in menus.
     */
    @SerializedName("image")
    private String image;
    
    /**
     * Fallback display name when localization is unavailable.
     * If not provided, the structure ID will be formatted as the name.
     */
    @SerializedName("fallback_name")
    private String fallbackName;
    
    /**
     * HTML hex color for UI display (e.g., "#4A4A4A").
     * Used for rendering structure indicators, trails, and menu items.
     */
    @SerializedName("color_html")
    private String colorHtml;
    
    /**
     * Reference to a scent ID from scents.json.
     * This scent will be emitted when tracking this structure.
     */
    @SerializedName("scent_id")
    private String scentId;
    
    /**
     * List of block IDs that are characteristic of this structure.
     * Can be used for structure identification or as tracking hints.
     * These should reference valid Minecraft block IDs.
     */
    @SerializedName("blocks")
    private List<String> blocks;
    
    /**
     * Default constructor for GSON deserialization.
     */
    public StructureDefinition() {
        this.blocks = new ArrayList<>();
    }
    
    /**
     * Constructor for programmatic creation.
     * 
     * @param structureId Minecraft structure ID
     * @param fallbackName Display name fallback
     * @param colorHtml HTML hex color
     * @param scentId Reference to scent definition
     */
    public StructureDefinition(String structureId, String fallbackName, String colorHtml, String scentId) {
        this.structureId = structureId;
        this.fallbackName = fallbackName;
        this.colorHtml = colorHtml;
        this.scentId = scentId;
        this.blocks = new ArrayList<>();
    }
    
    /**
     * Get the color with validation and fallback.
     * 
     * @return Valid HTML color, or default gray if invalid
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
        return 0x808080; // Default gray
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
     * @return Fallback name, or formatted structure ID if not set
     */
    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }
        // Auto-generate from structure ID
        if (structureId != null && !structureId.isEmpty()) {
            return formatStructureIdAsName(structureId);
        }
        return "Unknown Structure";
    }
    
    /**
     * Get the blocks list with null safety.
     * 
     * @return List of block IDs, never null
     */
    public List<String> getBlocks() {
        return blocks != null ? blocks : Collections.emptyList();
    }
    
    /**
     * Check if this structure has associated blocks.
     * 
     * @return true if blocks list is non-empty
     */
    public boolean hasBlocks() {
        return blocks != null && !blocks.isEmpty();
    }
    
    /**
     * Get the localization key for this structure's display name.
     * 
     * @return Localization key in format "structure.aromaaffect.namespace.path"
     */
    public String getTranslationKey() {
        if (structureId == null || !structureId.contains(":")) {
            return "structure.aromaaffect.unknown";
        }
        String[] parts = structureId.split(":", 2);
        return "structure.aromaaffect." + parts[0] + "." + parts[1];
    }
    
    /**
     * Get the namespace portion of the structure ID.
     * 
     * @return Namespace (e.g., "minecraft"), or empty string if invalid
     */
    public String getNamespace() {
        if (structureId != null && structureId.contains(":")) {
            return structureId.split(":", 2)[0];
        }
        return "";
    }
    
    /**
     * Get the path portion of the structure ID.
     * 
     * @return Path (e.g., "stronghold"), or the full ID if no namespace
     */
    public String getPath() {
        if (structureId != null && structureId.contains(":")) {
            return structureId.split(":", 2)[1];
        }
        return structureId != null ? structureId : "";
    }
    
    /**
     * Check if this is a vanilla Minecraft structure.
     * 
     * @return true if namespace is "minecraft"
     */
    public boolean isVanilla() {
        return "minecraft".equals(getNamespace());
    }
    
    /**
     * Validates the structure definition has required fields.
     * 
     * @return true if the definition is valid
     */
    public boolean isValid() {
        return structureId != null && !structureId.isEmpty();
    }
    
    /**
     * Check if the structure_id has valid ResourceLocation format.
     * 
     * @return true if structure_id matches namespace:path format
     */
    public boolean hasValidStructureIdFormat() {
        return structureId != null && RESOURCE_LOCATION_PATTERN.matcher(structureId).matches();
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
     * Format a structure ID as a display name.
     * Example: "minecraft:village_plains" -> "Village Plains"
     * 
     * @param structureId The structure ID to format
     * @return Formatted display name
     */
    private static String formatStructureIdAsName(String structureId) {
        // Remove namespace
        String path = structureId.contains(":") ? structureId.split(":", 2)[1] : structureId;
        
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
        StructureDefinition that = (StructureDefinition) o;
        return structureId != null && structureId.equals(that.structureId);
    }
    
    @Override
    public int hashCode() {
        return structureId != null ? structureId.hashCode() : 0;
    }
}

