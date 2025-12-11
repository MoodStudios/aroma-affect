package com.ovrtechnology.nose;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a crafting recipe for a nose item.
 * Supports both shaped (3x3 matrix) and shapeless recipes.
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class NoseRecipe {
    
    /**
     * Recipe type: "shaped" or "shapeless"
     */
    @SerializedName("type")
    private String type;
    
    /**
     * For shaped recipes: 3x3 pattern using single-character keys
     * Example: ["ABA", " C ", "   "] where A, B, C are keys in the "key" map
     */
    @SerializedName("pattern")
    private List<String> pattern;
    
    /**
     * Key mapping for shaped recipes.
     * Maps single characters to item IDs (e.g., {"A": "minecraft:iron_ingot"})
     */
    @SerializedName("key")
    private Map<String, String> key;
    
    /**
     * For shapeless recipes: list of ingredient item IDs
     */
    @SerializedName("ingredients")
    private List<String> ingredients;
    
    /**
     * Result count (defaults to 1)
     */
    @SerializedName("count")
    private int count;
    
    // Getters with defaults
    
    public String getType() {
        return type != null ? type : "shaped";
    }
    
    public boolean isShaped() {
        return "shaped".equalsIgnoreCase(getType());
    }
    
    public boolean isShapeless() {
        return "shapeless".equalsIgnoreCase(getType());
    }
    
    public List<String> getPattern() {
        return pattern != null ? pattern : Collections.emptyList();
    }
    
    public Map<String, String> getKey() {
        return key != null ? key : Collections.emptyMap();
    }
    
    public List<String> getIngredients() {
        return ingredients != null ? ingredients : Collections.emptyList();
    }
    
    public int getCount() {
        return count > 0 ? count : 1;
    }
    
    /**
     * Validates the recipe definition
     */
    public boolean isValid() {
        if (isShaped()) {
            return pattern != null && !pattern.isEmpty() && key != null && !key.isEmpty();
        } else if (isShapeless()) {
            return ingredients != null && !ingredients.isEmpty();
        }
        return false;
    }
    
    /**
     * Get all item IDs used in this recipe
     */
    public List<String> getAllItemIds() {
        if (isShaped()) {
            return key != null ? List.copyOf(key.values()) : Collections.emptyList();
        } else {
            return getIngredients();
        }
    }
}
