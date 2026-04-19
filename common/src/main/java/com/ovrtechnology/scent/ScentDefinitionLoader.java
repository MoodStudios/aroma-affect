package com.ovrtechnology.scent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads scent definitions from JSON files.
 * 
 * <p>This loader handles parsing the scent configuration from
 * {@code data/aromaaffect/scents/scents.json} and provides validation
 * for duplicate IDs and invalid entries.</p>
 * 
 * <p>The JSON format supports both array format and object with "scents" array:</p>
 * <pre>
 * // Array format:
 * [
 *   { "id": "winter", "fallback_name": "Winter" },
 *   { "id": "beach", "fallback_name": "Beach" }
 * ]
 * 
 * // Object format:
 * {
 *   "scents": [
 *     { "id": "winter", "fallback_name": "Winter" },
 *     { "id": "beach", "fallback_name": "Beach" }
 *   ]
 * }
 * </pre>
 */
public class ScentDefinitionLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    public static final String SCENTS_DIR = "aroma_scents";
    
    /**
     * Cached list of loaded scent definitions
     */
    @Getter
    private static List<ScentDefinition> loadedScents = new ArrayList<>();
    
    /**
     * Set of loaded scent IDs for duplicate detection
     */
    private static Set<String> loadedIds = new HashSet<>();
    
    /**
     * Load all scent definitions from the scents directory.
     * This parses the JSON file and validates each entry.
     * 
     * <p>Duplicate IDs are detected and logged as warnings, with only
     * the first occurrence being kept.</p>
     * 
     * @return An unmodifiable list of valid scent definitions
     */
    public static List<ScentDefinition> loadAllScents() {
        return loadAllScents(ClasspathDataSource.INSTANCE);
    }

    /**
     * Load all scent definitions from the given {@link DataSource}.
     * The classpath overload is preserved for mod-init-time loading;
     * reload listeners pass a {@code ResourceManagerDataSource} so that
     * installed datapacks and {@code /reload} are honored.
     */
    public static List<ScentDefinition> loadAllScents(DataSource dataSource) {
        loadedScents.clear();
        loadedIds.clear();

        Map<ResourceLocation, JsonElement> files = dataSource.listJson(SCENTS_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                ScentDefinition scent = GSON.fromJson(entry.getValue(), ScentDefinition.class);
                processScent(scent);
            } catch (Exception e) {
                AromaAffect.LOGGER.error("Failed to parse scent {}: {}", entry.getKey(), e.getMessage());
            }
        }

        AromaAffect.LOGGER.info("Loaded {} scent definitions from {} file(s)", loadedScents.size(), files.size());
        return Collections.unmodifiableList(loadedScents);
    }
    
    /**
     * Process a single scent definition, validating and adding to the loaded list.
     * 
     * @param scent The scent definition to process
     */
    private static void processScent(ScentDefinition scent) {
        if (scent == null) {
            AromaAffect.LOGGER.warn("Null scent definition found, skipping...");
            return;
        }
        
        if (!scent.isValid()) {
            AromaAffect.LOGGER.warn("Invalid scent definition found (missing id), skipping...");
            return;
        }
        
        String id = scent.getId();
        
        // Check for duplicate IDs
        if (loadedIds.contains(id)) {
            AromaAffect.LOGGER.warn("Duplicate scent ID '{}' found, skipping...", id);
            return;
        }
        
        // Validate and log the entry
        validateScent(scent);
        
        loadedIds.add(id);
        loadedScents.add(scent);
        AromaAffect.LOGGER.debug("Loaded scent definition: {} ({})", id, scent.getFallbackName());
    }
    
    /**
     * Validate a scent definition and log any warnings.
     * 
     * @param scent The scent to validate
     */
    private static void validateScent(ScentDefinition scent) {
        String id = scent.getId();
        
        // Check fallback name
        if (scent.getFallbackName() == null || scent.getFallbackName().isEmpty()) {
            AromaAffect.LOGGER.warn("[{}] No fallback_name defined, will use auto-generated name", id);
        }
    }
    
    
    /**
     * Parse a single scent definition from a JSON string.
     * Useful for testing or dynamic scent creation.
     * 
     * @param json The JSON string to parse
     * @return The parsed scent definition, or null if parsing fails
     */
    public static ScentDefinition parseScentFromJson(String json) {
        try {
            return GSON.fromJson(json, ScentDefinition.class);
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to parse scent definition from JSON", e);
            return null;
        }
    }
    
    /**
     * Get a scent definition by ID from the loaded scents.
     * 
     * @param id The scent ID to look up
     * @return The scent definition, or null if not found
     */
    public static ScentDefinition getScentById(String id) {
        for (ScentDefinition scent : loadedScents) {
            if (scent.getId().equals(id)) {
                return scent;
            }
        }
        return null;
    }
    
    /**
     * Check if a scent with the given ID has been loaded.
     * 
     * @param id The scent ID to check
     * @return true if the scent exists
     */
    public static boolean hasScentId(String id) {
        return loadedIds.contains(id);
    }
    
    /**
     * Serialize a scent definition to JSON (useful for debugging/export).
     * 
     * @param scent The scent to serialize
     * @return JSON string representation
     */
    public static String toJson(ScentDefinition scent) {
        return GSON.toJson(scent);
    }
    
    /**
     * Get the GSON instance for external use.
     * 
     * @return The configured GSON instance
     */
    public static Gson getGson() {
        return GSON;
    }
    
    /**
     * Reload all scent definitions.
     * This clears the cache and reloads from the JSON file.
     * 
     * @return The reloaded list of scent definitions
     */
    public static List<ScentDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading scent definitions...");
        return loadAllScents();
    }
}

