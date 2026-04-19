package com.ovrtechnology.scentitem;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Represents a scent item definition loaded from JSON.
 * 
 * <p>Each scent item is a Minecraft item that represents a physical scent container.
 * These are separate from the OVR scent definitions used for hardware communication.</p>
 * 
 * <p>Example JSON entry:</p>
 * <pre>
 * {
 *   "id": "winter_scent",
 *   "image": "item/scent_winter",
 *   "model": "minecraft:light_blue_dye",
 *   "fallback_name": "Winter Scent",
 *   "description": "Breathing in cold air...",
 *   "priority": 5
 * }
 * </pre>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class ScentItemDefinition {
    
    /**
     * Unique identifier for this scent item (e.g., "winter_scent").
     * Used for item registration and localization keys.
     */
    @SerializedName("id")
    private String id;

    /**
     * The type of scent item. "capsule" indicates an aroma capsule that provides
     * a specific scent. Items without this field are utility items (containers, bases, etc.).
     */
    @SerializedName("type")
    private String type;

    /**
     * The name of the scent this item provides (e.g., "Winter", "Floral").
     * Only meaningful for capsule-type items.
     */
    @SerializedName("scent")
    private String scent;

    /**
     * Path to the texture file relative to assets/aromaaffect/textures/
     * Example: "item/scent_winter"
     */
    @SerializedName("image")
    private String image;
    
    /**
     * Minecraft item model to use as base (e.g., "minecraft:light_blue_dye").
     * This determines the item's appearance in-game.
     */
    @SerializedName("model")
    private String model;
    
    /**
     * Fallback display name when localization is not available.
     */
    @SerializedName("fallback_name")
    private String fallbackName;
    
    /**
     * Description of the scent for tooltips.
     */
    @SerializedName("description")
    private String description;
    
    /**
     * Priority level for scent emission (1-10, higher = more important).
     */
    @SerializedName("priority")
    private int priority;

    /**
     * Whether this scent item is visible/functional. Built-in entries default to
     * {@code true}. Slot entries pre-registered for datapack customization default
     * to {@code false} and flip to {@code true} when a datapack supplies data.
     */
    @SerializedName("enabled")
    private boolean enabled = true;

    /**
     * Default constructor for GSON deserialization.
     */
    public ScentItemDefinition() {
        this.priority = 5; // Default middle priority
    }
    
    /**
     * Get the fallback name with a sensible default.
     * 
     * @return The fallback name, or the ID formatted if not set
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
     * Get the model with default fallback.
     * 
     * @return The model ID, or default if not set
     */
    public String getModel() {
        return model != null && !model.isEmpty() ? model : "minecraft:paper";
    }
    
    /**
     * Get the image path with default fallback.
     * 
     * @return The image path, or default if not set
     */
    public String getImage() {
        return image != null && !image.isEmpty() ? image : "item/scent_default";
    }
    
    /**
     * Get the priority with bounds checking.
     * 
     * @return Priority clamped between 1 and 10
     */
    public int getPriority() {
        return Math.max(1, Math.min(10, priority > 0 ? priority : 5));
    }
    
    /**
     * Get the localization key for this scent item's name.
     * 
     * @return The localization key in format "item.aromaaffect.{id}"
     */
    public String getTranslationKey() {
        return "item.aromaaffect." + id;
    }
    
    /**
     * Get the localization key for this scent item's description.
     * 
     * @return The localization key in format "scent.aromaaffect.{id}.description"
     */
    public String getDescriptionTranslationKey() {
        return "scent.aromaaffect." + id + ".description";
    }
    
    /**
     * Whether this scent item is an aroma capsule.
     *
     * @return true if the type is "capsule"
     */
    public boolean isCapsule() {
        return "capsule".equals(type);
    }

    /**
     * Validates the scent item definition has required fields.
     * 
     * @return true if the definition is valid
     */
    public boolean isValid() {
        return id != null && !id.isEmpty();
    }
    
    /**
     * Convert snake_case ID to Title Case name.
     * Example: "winter_scent" -> "Winter Scent"
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
        ScentItemDefinition that = (ScentItemDefinition) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
