package com.ovrtechnology.sniffernose;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Represents a sniffer nose item definition loaded from JSON.
 * 
 * <p>Sniffer noses are items designed for the Sniffer mob, not for player equipment.
 * They are regular items that can be used in crafting, dropped by mobs, etc.</p>
 * 
 * <p>Example JSON entry:</p>
 * <pre>
 * {
 *   "id": "enhanced_sniffer_nose",
 *   "image": "item/enhanced_sniffer_nose",
 *   "model": "minecraft:leather_helmet",
 *   "tier": 1,
 *   "durability": 100
 * }
 * </pre>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class SnifferNoseDefinition {
    
    /**
     * Unique identifier for this sniffer nose (e.g., "enhanced_sniffer_nose")
     */
    @SerializedName("id")
    private String id;
    
    /**
     * Path to the texture file relative to assets/aromacraft/textures/
     */
    @SerializedName("image")
    private String image;
    
    /**
     * Model type for the item appearance
     */
    @SerializedName("model")
    private String model;
    
    /**
     * Tier level of this sniffer nose
     */
    @SerializedName("tier")
    private int tier;
    
    /**
     * Durability value (for reference, not used as equipment)
     */
    @SerializedName("durability")
    private int durability;
    
    /**
     * Item ID used for repairing (optional)
     */
    @SerializedName("repair")
    private String repair;
    
    /**
     * Default constructor for GSON deserialization
     */
    public SnifferNoseDefinition() {
        this.tier = 1;
        this.durability = 100;
    }
    
    /**
     * Get the model with default fallback
     */
    public String getModel() {
        return model != null && !model.isEmpty() ? model : "minecraft:leather_helmet";
    }
    
    /**
     * Get the image path with default fallback
     */
    public String getImage() {
        return image != null && !image.isEmpty() ? image : "item/sniffer_nose_default";
    }
    
    /**
     * Get the durability with default
     */
    public int getDurability() {
        return durability > 0 ? durability : 100;
    }
    
    /**
     * Get the localization key for this item's name
     */
    public String getTranslationKey() {
        return "item.aromacraft." + id;
    }
    
    /**
     * Validates the definition has required fields
     */
    public boolean isValid() {
        return id != null && !id.isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnifferNoseDefinition that = (SnifferNoseDefinition) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
