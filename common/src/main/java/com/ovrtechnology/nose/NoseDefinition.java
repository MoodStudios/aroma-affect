package com.ovrtechnology.nose;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Represents a nose item definition loaded from JSON.
 * Each nose has properties like tier, durability, and unlock conditions.
 * Recipes are defined separately in data/aromacraft/recipe/ as standard Minecraft recipe JSON files.
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class NoseDefinition {
    
    /**
     * Unique identifier for this nose (e.g., "basic_nose", "iron_nose")
     */
    @SerializedName("id")
    private String id;
    
    /**
     * Path to the texture file relative to assets/aromacraft/textures/item/
     */
    @SerializedName("image")
    private String image;
    
    /**
     * Model type for the nose (similar to armor models)
     * Can be: "basic", "advanced", "elite", or custom model identifiers
     * Defaults to iron_helmet model if not specified
     */
    @SerializedName("model")
    private String model;
    
    /**
     * Unlock conditions for this nose
     */
    @SerializedName("unlock")
    private NoseUnlock unlock;
    
    /**
     * Durability of the nose item (number of uses before breaking)
     */
    @SerializedName("durability")
    private int durability;
    
    /**
     * Item ID used for repairing this nose (e.g., "minecraft:iron_ingot")
     */
    @SerializedName("repair")
    private String repair;
    
    /**
     * Tier level of this nose. Higher tiers unlock more capabilities.
     * Each tier unlocks the next tier level.
     */
    @SerializedName("tier")
    private int tier;
    
    // Override getters with defaults
    
    public String getModel() {
        return model != null && !model.isEmpty() ? model : "minecraft:iron_helmet";
    }
    
    public int getDurability() {
        return durability > 0 ? durability : 100;
    }
    
    public NoseUnlock getUnlock() {
        return unlock != null ? unlock : new NoseUnlock();
    }
    
    /**
     * Validates the nose definition has required fields
     */
    public boolean isValid() {
        return id != null && !id.isEmpty();
    }
}
