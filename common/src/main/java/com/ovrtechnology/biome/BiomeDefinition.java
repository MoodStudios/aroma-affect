package com.ovrtechnology.biome;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.definition.ImageDefinition;
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
 * <p>Biome IDs follow the Minecraft Identifier format: {@code namespace:path}.
 * For vanilla biomes, use {@code minecraft:} namespace. For modded biomes,
 * use the mod's namespace (e.g., {@code terralith:hot_shrubland}).</p>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class BiomeDefinition extends ImageDefinition {

    public BiomeDefinition() {
        super();
    }

    public BiomeDefinition(String id, String fallbackName, String color, String scentId) {
        super(id, fallbackName, color, scentId);
    }

    @Override
    protected String getDefaultColor() {
        return "5AA000";
    }

    @Override
    protected String getFallbackTitle() {
        return "Biome";
    }

    @Override
    protected String getTranslationTitle() {
        return "biome";
    }
}

