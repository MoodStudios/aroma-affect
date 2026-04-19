package com.ovrtechnology.scentitem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.data.JsonResources;
import lombok.Getter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads scent item definitions from JSON files.
 * 
 * <p>This loader handles parsing the scent item configuration from
 * {@code data/aromaaffect/scents/scent_items.json}.</p>
 * 
 * <p>The JSON format supports both array format and object with "scents" array:</p>
 * <pre>
 * {
 *   "scents": [
 *     { "id": "winter_scent", "image": "item/scent_winter", "model": "minecraft:light_blue_dye", ... },
 *     { "id": "beach_scent", "image": "item/scent_beach", "model": "minecraft:nautilus_shell", ... }
 *   ]
 * }
 * </pre>
 */
public class ScentItemDefinitionLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Path to the scent items JSON file
     */
    private static final String SCENT_ITEMS_RESOURCE_PATH = "data/aromaaffect/scents/scent_items.json";
    
    /**
     * Default texture path for fallback
     */
    private static final String DEFAULT_TEXTURE = "item/scent_default";
    
    /**
     * Cached list of loaded scent item definitions
     */
    @Getter
    private static List<ScentItemDefinition> loadedScentItems = new ArrayList<>();
    
    /**
     * Set of loaded scent item IDs for duplicate detection
     */
    private static Set<String> loadedIds = new HashSet<>();
    
    /**
     * Load all scent item definitions from the JSON file.
     * 
     * @return An unmodifiable list of valid scent item definitions
     */
    public static List<ScentItemDefinition> loadAllScentItems() {
        return loadAllScentItems(ClasspathDataSource.INSTANCE);
    }

    public static List<ScentItemDefinition> loadAllScentItems(DataSource dataSource) {
        loadedScentItems.clear();
        loadedIds.clear();

        try {
            ScentItemDefinition[] scentItems = loadScentItemsFromResource(dataSource, SCENT_ITEMS_RESOURCE_PATH);
            if (scentItems != null) {
                for (ScentItemDefinition scentItem : scentItems) {
                    processScentItem(scentItem);
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load scent item definitions", e);
        }

        AromaAffect.LOGGER.info("Loaded {} scent item definitions", loadedScentItems.size());
        return Collections.unmodifiableList(loadedScentItems);
    }

    /**
     * In-place reload variant. See {@code NoseDefinitionLoader#reloadInPlace}.
     */
    public static void reloadInPlace(DataSource dataSource) {
        ScentItemDefinition[] newDefs = loadScentItemsFromResource(dataSource, SCENT_ITEMS_RESOURCE_PATH);
        int mutated = 0;
        int skipped = 0;
        for (ScentItemDefinition src : newDefs) {
            if (src == null || !src.isValid()) continue;
            ScentItemDefinition target = findLoadedById(src.getId());
            if (target == null) {
                AromaAffect.LOGGER.warn(
                        "[ScentItem reload] ID '{}' is not a built-in; custom scent items must use the variant system.",
                        src.getId());
                skipped++;
                continue;
            }
            copyFields(src, target);
            mutated++;
        }
        AromaAffect.LOGGER.info("ScentItem reload: mutated {} definitions in place, skipped {} unknown IDs",
                mutated, skipped);
    }

    private static ScentItemDefinition findLoadedById(String id) {
        for (ScentItemDefinition d : loadedScentItems) {
            if (id.equals(d.getId())) return d;
        }
        return null;
    }

    private static void copyFields(ScentItemDefinition src, ScentItemDefinition dst) {
        dst.setImage(src.getImage());
        dst.setModel(src.getModel());
        dst.setType(src.getType());
        dst.setScent(src.getScent());
        dst.setFallbackName(src.getFallbackName());
        dst.setDescription(src.getDescription());
        dst.setPriority(src.getPriority());
        dst.setEnabled(src.isEnabled());
    }
    
    /**
     * Process a single scent item definition, validating and adding to the loaded list.
     */
    private static void processScentItem(ScentItemDefinition scentItem) {
        if (scentItem == null) {
            AromaAffect.LOGGER.warn("Null scent item definition found, skipping...");
            return;
        }
        
        if (!scentItem.isValid()) {
            AromaAffect.LOGGER.warn("Invalid scent item definition found (missing id), skipping...");
            return;
        }
        
        String id = scentItem.getId();
        
        // Check for duplicate IDs
        if (loadedIds.contains(id)) {
            AromaAffect.LOGGER.warn("Duplicate scent item ID '{}' found, skipping...", id);
            return;
        }
        
        // Validate and apply fallbacks
        validateAndApplyFallbacks(scentItem);
        
        loadedIds.add(id);
        loadedScentItems.add(scentItem);
        AromaAffect.LOGGER.debug("Loaded scent item definition: {} ({})", id, scentItem.getFallbackName());
    }
    
    /**
     * Validate scent item definition and apply fallbacks for missing values.
     */
    private static void validateAndApplyFallbacks(ScentItemDefinition scentItem) {
        String id = scentItem.getId();
        
        // Check texture
        String texturePath = scentItem.getImage();
        if (texturePath == null || texturePath.isEmpty()) {
            AromaAffect.LOGGER.warn("[{}] No texture defined, using fallback: {}", id, DEFAULT_TEXTURE);
            scentItem.setImage(DEFAULT_TEXTURE);
        } else if (!textureExists(texturePath)) {
            AromaAffect.LOGGER.warn("[{}] Texture '{}' not found, using fallback: {}", id, texturePath, DEFAULT_TEXTURE);
            scentItem.setImage(DEFAULT_TEXTURE);
        }
        
        // Check model
        if (scentItem.getModel() == null || scentItem.getModel().isEmpty()) {
            AromaAffect.LOGGER.info("[{}] No model defined, using default: minecraft:paper", id);
        }
        
        // Check priority bounds
        int priority = scentItem.getPriority();
        if (priority < 1 || priority > 10) {
            AromaAffect.LOGGER.warn("[{}] Priority {} out of bounds (1-10), will be clamped", id, priority);
        }
    }
    
    /**
     * Check if a texture file exists in the resources.
     */
    private static boolean textureExists(String texturePath) {
        String fullPath = "assets/aromaaffect/textures/" + texturePath + ".png";
        
        try (InputStream stream = ScentItemDefinitionLoader.class.getClassLoader().getResourceAsStream(fullPath)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static ScentItemDefinition[] loadScentItemsFromResource(DataSource dataSource, String resourcePath) {
        JsonElement element = dataSource.read(resourcePath);
        if (element == null) {
            AromaAffect.LOGGER.warn("Scent items definitions file not found: {}", resourcePath);
            return new ScentItemDefinition[0];
        }
        return JsonResources.parseArrayOrWrapped(element, "scents", ScentItemDefinition[].class, GSON, resourcePath);
    }
    
    /**
     * Get a scent item definition by ID from the loaded items.
     * 
     * @param id The scent item ID to look up
     * @return The scent item definition, or null if not found
     */
    public static ScentItemDefinition getScentItemById(String id) {
        for (ScentItemDefinition scentItem : loadedScentItems) {
            if (scentItem.getId().equals(id)) {
                return scentItem;
            }
        }
        return null;
    }
    
    /**
     * Check if a scent item with the given ID has been loaded.
     * 
     * @param id The scent item ID to check
     * @return true if the scent item exists
     */
    public static boolean hasScentItemId(String id) {
        return loadedIds.contains(id);
    }
    
    /**
     * Serialize a scent item definition to JSON.
     */
    public static String toJson(ScentItemDefinition scentItem) {
        return GSON.toJson(scentItem);
    }
    
    /**
     * Get the GSON instance for external use.
     */
    public static Gson getGson() {
        return GSON;
    }
}
