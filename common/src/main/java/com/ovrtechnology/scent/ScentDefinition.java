package com.ovrtechnology.scent;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Represents a scent definition loaded from JSON.
 * 
 * <p>Each scent corresponds to an OVR hardware scent identifier and has a fallback
 * display name for when localization is not available. The actual display name
 * should be retrieved from Minecraft's localization system using the key format:
 * {@code scent.aromaaffect.<id>}</p>
 * 
 * <p>Example JSON entry:</p>
 * <pre>
 * {
 *   "id": "winter",
 *   "fallback_name": "Winter"
 * }
 * </pre>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class ScentDefinition {
    
    /**
     * Unique identifier for this scent.
     * This ID is used for:
     * - OVR hardware communication
     * - Localization key lookup (scent.aromaaffect.{id})
     * - Internal references and configuration
     */
    @SerializedName("id")
    private String id;
    
    /**
     * Fallback display name when localization is not available.
     * This ensures the scent always has a human-readable name.
     */
    @SerializedName("fallback_name")
    private String fallbackName;
    
    /**
     * Optional description of the scent for tooltips.
     * If not provided, falls back to localization key: scent.aromaaffect.{id}.description
     */
    @SerializedName("description")
    private String description;

    /**
     * Hex color string for this scent (e.g., "#FF85A1").
     * Used for particle coloring and UI elements.
     */
    @SerializedName("color")
    private String color;
    
    /**
     * Default constructor for GSON deserialization.
     */
    public ScentDefinition() {
    }
    
    /**
     * Constructor for programmatic creation.
     * 
     * @param id Unique scent identifier
     * @param fallbackName Fallback display name
     */
    public ScentDefinition(String id, String fallbackName) {
        this.id = id;
        this.fallbackName = fallbackName;
    }
    
    /**
     * Get the fallback name with a sensible default.
     * 
     * @return The fallback name, or the ID capitalized if not set
     */
    public String getFallbackName() {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }
        // Auto-generate fallback from ID (snake_case to Title Case)
        if (id != null && !id.isEmpty()) {
            return formatIdAsName(id);
        }
        return "Unknown Scent";
    }
    
    /**
     * Get the localization key for this scent's name.
     * 
     * @return The localization key in format "scent.aromaaffect.{id}"
     */
    public String getTranslationKey() {
        return "scent.aromaaffect." + id;
    }
    
    /**
     * Get the localization key for this scent's description.
     * 
     * @return The localization key in format "scent.aromaaffect.{id}.description"
     */
    public String getDescriptionTranslationKey() {
        return "scent.aromaaffect." + id + ".description";
    }
    
    /**
     * Parse the hex color string into an RGB int array [r, g, b] (0-255).
     * Returns white (255,255,255) if the color is not set or invalid.
     */
    public int[] getColorRGB() {
        if (color == null || color.isEmpty()) {
            return new int[]{255, 255, 255};
        }
        try {
            String hex = color.startsWith("#") ? color.substring(1) : color;
            int rgb = Integer.parseInt(hex, 16);
            return new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
        } catch (NumberFormatException e) {
            return new int[]{255, 255, 255};
        }
    }

    /**
     * Validates the scent definition has required fields.
     * 
     * @return true if the definition is valid
     */
    public boolean isValid() {
        return id != null && !id.isEmpty();
    }
    
    /**
     * Convert snake_case ID to Title Case name.
     * Example: "terra_silva" -> "Terra Silva"
     */
    private static String formatIdAsName(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        
        String[] parts = id.split("_");
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
        ScentDefinition that = (ScentDefinition) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

