package com.ovrtechnology.biome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.AromaCraft;
import com.ovrtechnology.scent.ScentRegistry;
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
 * Loads trackable biome definitions from JSON files.
 * 
 * <p>This loader handles parsing the biome configuration from
 * {@code data/aromacraft/biomes/biomes.json} and provides validation
 * for duplicate IDs, HTML color format, and scent references.</p>
 * 
 * <p>The JSON format supports both array format and object with "biomes" array:</p>
 * <pre>
 * // Array format:
 * [
 *   { "biome_id": "minecraft:jungle", "color_html": "#537B09", "scent_id": "evergreen" }
 * ]
 * 
 * // Object format:
 * {
 *   "biomes": [
 *     { "biome_id": "minecraft:jungle", "color_html": "#537B09", "scent_id": "evergreen" }
 *   ]
 * }
 * </pre>
 * 
 * <h2>Validation</h2>
 * <ul>
 *   <li>Duplicate biome_id detection (first occurrence kept)</li>
 *   <li>Biome ID format validation (namespace:path)</li>
 *   <li>HTML color format validation (#RGB, #RRGGBB, #RRGGBBAA)</li>
 *   <li>Scent ID validation against ScentRegistry</li>
 * </ul>
 */
public class BiomeDefinitionLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Path to the biomes JSON file
     */
    private static final String BIOMES_RESOURCE_PATH = "data/aromacraft/biomes/biomes.json";
    
    /**
     * Cached list of loaded biome definitions
     */
    @Getter
    private static List<BiomeDefinition> loadedBiomes = new ArrayList<>();
    
    /**
     * Set of loaded biome IDs for duplicate detection
     */
    private static Set<String> loadedIds = new HashSet<>();
    
    /**
     * List of validation warnings encountered during loading
     */
    @Getter
    private static List<String> validationWarnings = new ArrayList<>();
    
    /**
     * Load all biome definitions from the biomes directory.
     * This parses the JSON file and validates each entry.
     * 
     * <p>Validation includes:</p>
     * <ul>
     *   <li>Duplicate biome_id detection</li>
     *   <li>Biome ID format (namespace:path)</li>
     *   <li>HTML color format validation</li>
     *   <li>Scent ID reference validation</li>
     * </ul>
     * 
     * @return An unmodifiable list of valid biome definitions
     */
    public static List<BiomeDefinition> loadAllBiomes() {
        loadedBiomes.clear();
        loadedIds.clear();
        validationWarnings.clear();
        
        try {
            BiomeDefinition[] biomes = loadBiomesFromResource(BIOMES_RESOURCE_PATH);
            if (biomes != null) {
                for (BiomeDefinition biome : biomes) {
                    processBiome(biome);
                }
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Failed to load biome definitions", e);
        }
        
        AromaCraft.LOGGER.info("Loaded {} biome definitions", loadedBiomes.size());
        
        if (!validationWarnings.isEmpty()) {
            AromaCraft.LOGGER.warn("Biome loading completed with {} validation warnings", validationWarnings.size());
        }
        
        return Collections.unmodifiableList(loadedBiomes);
    }
    
    /**
     * Process a single biome definition, validating and adding to the loaded list.
     * 
     * @param biome The biome definition to process
     */
    private static void processBiome(BiomeDefinition biome) {
        if (biome == null) {
            addWarning("Null biome definition found, skipping...");
            return;
        }
        
        if (!biome.isValid()) {
            addWarning("Invalid biome definition found (missing biome_id), skipping...");
            return;
        }
        
        String biomeId = biome.getBiomeId();
        
        // Check for duplicate IDs
        if (loadedIds.contains(biomeId)) {
            addWarning("Duplicate biome_id '" + biomeId + "' found, skipping...");
            return;
        }
        
        // Validate the biome entry
        validateBiome(biome);
        
        loadedIds.add(biomeId);
        loadedBiomes.add(biome);
        AromaCraft.LOGGER.debug("Loaded biome definition: {} (color: {}, scent: {})", 
                biomeId, biome.getColorHtml(), biome.getScentId());
    }
    
    /**
     * Validate a biome definition and log any warnings.
     * 
     * @param biome The biome to validate
     */
    private static void validateBiome(BiomeDefinition biome) {
        String biomeId = biome.getBiomeId();
        
        // Validate biome_id format (namespace:path)
        if (!biome.hasValidBiomeIdFormat()) {
            addWarning("[" + biomeId + "] Invalid biome_id format - should be 'namespace:path' (e.g., 'minecraft:jungle')");
        }
        
        // Validate HTML color format
        String rawColor = biome.getRawColorHtml();
        if (rawColor == null || rawColor.isEmpty()) {
            addWarning("[" + biomeId + "] No color_html defined, using default: " + BiomeDefinition.DEFAULT_COLOR);
        } else if (!BiomeDefinition.isValidHtmlColor(rawColor)) {
            addWarning("[" + biomeId + "] Invalid color_html format '" + rawColor + "', using default: " + BiomeDefinition.DEFAULT_COLOR);
        }
        
        // Validate scent_id reference
        if (biome.hasScentId()) {
            String scentId = biome.getScentId();
            if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(scentId)) {
                addWarning("[" + biomeId + "] Referenced scent_id '" + scentId + "' does not exist in ScentRegistry");
            }
        } else {
            addWarning("[" + biomeId + "] No scent_id defined, biome will have no associated scent");
        }
        
        // Validate image path
        String rawImage = biome.getRawImage();
        if (rawImage == null || rawImage.isEmpty()) {
            AromaCraft.LOGGER.debug("[{}] No image defined, using default: {}", biomeId, BiomeDefinition.DEFAULT_IMAGE);
        }
    }
    
    /**
     * Add a validation warning.
     * 
     * @param warning The warning message
     */
    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaCraft.LOGGER.warn(warning);
    }
    
    /**
     * Load biome definitions from a specific JSON resource file.
     * 
     * @param resourcePath Path to the JSON resource
     * @return Array of biome definitions, or empty array if not found
     */
    private static BiomeDefinition[] loadBiomesFromResource(String resourcePath) {
        try (InputStream inputStream = BiomeDefinitionLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                AromaCraft.LOGGER.warn("Biome definitions file not found: {}", resourcePath);
                return new BiomeDefinition[0];
            }
            
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                
                // Support both array format and object with "biomes" array
                if (jsonElement.isJsonArray()) {
                    return GSON.fromJson(jsonElement, BiomeDefinition[].class);
                } else if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("biomes")) {
                        return GSON.fromJson(jsonObject.get("biomes"), BiomeDefinition[].class);
                    }
                }
                
                AromaCraft.LOGGER.warn("Invalid JSON format in: {} (expected array or object with 'biomes' key)", resourcePath);
                return new BiomeDefinition[0];
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Error parsing biome definitions from: {}", resourcePath, e);
            return new BiomeDefinition[0];
        }
    }
    
    /**
     * Parse a single biome definition from a JSON string.
     * 
     * @param json The JSON string to parse
     * @return The parsed biome definition, or null if parsing fails
     */
    public static BiomeDefinition parseBiomeFromJson(String json) {
        try {
            return GSON.fromJson(json, BiomeDefinition.class);
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Failed to parse biome definition from JSON", e);
            return null;
        }
    }
    
    /**
     * Get a biome definition by biome ID from the loaded biomes.
     * 
     * @param biomeId The biome ID to look up
     * @return The biome definition, or null if not found
     */
    public static BiomeDefinition getBiomeById(String biomeId) {
        for (BiomeDefinition biome : loadedBiomes) {
            if (biome.getBiomeId().equals(biomeId)) {
                return biome;
            }
        }
        return null;
    }
    
    /**
     * Check if a biome with the given ID has been loaded.
     * 
     * @param biomeId The biome ID to check
     * @return true if the biome exists
     */
    public static boolean hasBiomeId(String biomeId) {
        return loadedIds.contains(biomeId);
    }
    
    /**
     * Get all biomes that reference a specific scent.
     * 
     * @param scentId The scent ID to filter by
     * @return List of biomes using the specified scent
     */
    public static List<BiomeDefinition> getBiomesByScentId(String scentId) {
        List<BiomeDefinition> result = new ArrayList<>();
        for (BiomeDefinition biome : loadedBiomes) {
            if (scentId.equals(biome.getScentId())) {
                result.add(biome);
            }
        }
        return result;
    }
    
    /**
     * Get all vanilla Minecraft biomes.
     * 
     * @return List of biomes with "minecraft" namespace
     */
    public static List<BiomeDefinition> getVanillaBiomes() {
        List<BiomeDefinition> result = new ArrayList<>();
        for (BiomeDefinition biome : loadedBiomes) {
            if (biome.isVanilla()) {
                result.add(biome);
            }
        }
        return result;
    }
    
    /**
     * Get all modded biomes (non-vanilla).
     * 
     * @return List of biomes without "minecraft" namespace
     */
    public static List<BiomeDefinition> getModdedBiomes() {
        List<BiomeDefinition> result = new ArrayList<>();
        for (BiomeDefinition biome : loadedBiomes) {
            if (!biome.isVanilla()) {
                result.add(biome);
            }
        }
        return result;
    }
    
    /**
     * Serialize a biome definition to JSON.
     * 
     * @param biome The biome to serialize
     * @return JSON string representation
     */
    public static String toJson(BiomeDefinition biome) {
        return GSON.toJson(biome);
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
     * Reload all biome definitions.
     * This clears the cache and reloads from the JSON file.
     * 
     * @return The reloaded list of biome definitions
     */
    public static List<BiomeDefinition> reload() {
        AromaCraft.LOGGER.info("Reloading biome definitions...");
        return loadAllBiomes();
    }
    
    /**
     * Check if any validation warnings occurred during loading.
     * 
     * @return true if there were validation warnings
     */
    public static boolean hasValidationWarnings() {
        return !validationWarnings.isEmpty();
    }
}

