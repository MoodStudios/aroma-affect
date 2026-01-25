package com.ovrtechnology.scent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.AromaAffect;
import lombok.Getter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    
    /**
     * Path to the scents JSON file
     */
    private static final String SCENTS_RESOURCE_PATH = "data/aromaaffect/scents/scents.json";
    
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
        loadedScents.clear();
        loadedIds.clear();
        
        try {
            ScentDefinition[] scents = loadScentsFromResource(SCENTS_RESOURCE_PATH);
            if (scents != null) {
                for (ScentDefinition scent : scents) {
                    processScent(scent);
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load scent definitions", e);
        }
        
        AromaAffect.LOGGER.info("Loaded {} scent definitions", loadedScents.size());
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
     * Load scent definitions from a specific JSON resource file.
     * 
     * @param resourcePath Path to the JSON resource
     * @return Array of scent definitions, or empty array if not found
     */
    private static ScentDefinition[] loadScentsFromResource(String resourcePath) {
        try (InputStream inputStream = ScentDefinitionLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                AromaAffect.LOGGER.warn("Scent definitions file not found: {}", resourcePath);
                return new ScentDefinition[0];
            }
            
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                
                // Support both array format and object with "scents" array
                if (jsonElement.isJsonArray()) {
                    return GSON.fromJson(jsonElement, ScentDefinition[].class);
                } else if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("scents")) {
                        return GSON.fromJson(jsonObject.get("scents"), ScentDefinition[].class);
                    }
                }
                
                AromaAffect.LOGGER.warn("Invalid JSON format in: {} (expected array or object with 'scents' key)", resourcePath);
                return new ScentDefinition[0];
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error parsing scent definitions from: {}", resourcePath, e);
            return new ScentDefinition[0];
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

