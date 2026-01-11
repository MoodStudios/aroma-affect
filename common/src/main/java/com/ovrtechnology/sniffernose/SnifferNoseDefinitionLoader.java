package com.ovrtechnology.sniffernose;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.AromaCraft;
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
 * Loads sniffer nose definitions from JSON files.
 * 
 * <p>This loader handles parsing the sniffer nose configuration from
 * {@code data/aromacraft/noses/sniffer_noses.json}.</p>
 */
public class SnifferNoseDefinitionLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Path to the sniffer noses JSON file
     */
    private static final String SNIFFER_NOSES_RESOURCE_PATH = "data/aromacraft/noses/sniffer_noses.json";
    
    /**
     * Default texture path for fallback
     */
    private static final String DEFAULT_TEXTURE = "item/sniffer_nose_default";
    
    /**
     * Cached list of loaded sniffer nose definitions
     */
    @Getter
    private static List<SnifferNoseDefinition> loadedSnifferNoses = new ArrayList<>();
    
    /**
     * Set of loaded IDs for duplicate detection
     */
    private static Set<String> loadedIds = new HashSet<>();
    
    /**
     * Load all sniffer nose definitions from the JSON file.
     * 
     * @return An unmodifiable list of valid sniffer nose definitions
     */
    public static List<SnifferNoseDefinition> loadAllSnifferNoses() {
        loadedSnifferNoses.clear();
        loadedIds.clear();
        
        try {
            SnifferNoseDefinition[] snifferNoses = loadSnifferNosesFromResource(SNIFFER_NOSES_RESOURCE_PATH);
            if (snifferNoses != null) {
                for (SnifferNoseDefinition snifferNose : snifferNoses) {
                    processSnifferNose(snifferNose);
                }
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Failed to load sniffer nose definitions", e);
        }
        
        AromaCraft.LOGGER.info("Loaded {} sniffer nose definitions", loadedSnifferNoses.size());
        return Collections.unmodifiableList(loadedSnifferNoses);
    }
    
    /**
     * Process a single sniffer nose definition
     */
    private static void processSnifferNose(SnifferNoseDefinition snifferNose) {
        if (snifferNose == null) {
            AromaCraft.LOGGER.warn("Null sniffer nose definition found, skipping...");
            return;
        }
        
        if (!snifferNose.isValid()) {
            AromaCraft.LOGGER.warn("Invalid sniffer nose definition found (missing id), skipping...");
            return;
        }
        
        String id = snifferNose.getId();
        
        // Check for duplicate IDs
        if (loadedIds.contains(id)) {
            AromaCraft.LOGGER.warn("Duplicate sniffer nose ID '{}' found, skipping...", id);
            return;
        }
        
        // Validate and apply fallbacks
        validateAndApplyFallbacks(snifferNose);
        
        loadedIds.add(id);
        loadedSnifferNoses.add(snifferNose);
        AromaCraft.LOGGER.debug("Loaded sniffer nose definition: {}", id);
    }
    
    /**
     * Validate definition and apply fallbacks for missing values
     */
    private static void validateAndApplyFallbacks(SnifferNoseDefinition snifferNose) {
        String id = snifferNose.getId();
        
        // Check texture
        String texturePath = snifferNose.getImage();
        if (texturePath == null || texturePath.isEmpty()) {
            AromaCraft.LOGGER.warn("[{}] No texture defined, using fallback: {}", id, DEFAULT_TEXTURE);
            snifferNose.setImage(DEFAULT_TEXTURE);
        } else if (!textureExists(texturePath)) {
            AromaCraft.LOGGER.warn("[{}] Texture '{}' not found, using fallback: {}", id, texturePath, DEFAULT_TEXTURE);
            snifferNose.setImage(DEFAULT_TEXTURE);
        }
        
        // Check model
        if (snifferNose.getModel() == null || snifferNose.getModel().isEmpty()) {
            AromaCraft.LOGGER.info("[{}] No model defined, using default: minecraft:leather_helmet", id);
        }
    }
    
    /**
     * Check if a texture file exists in the resources
     */
    private static boolean textureExists(String texturePath) {
        String fullPath = "assets/aromacraft/textures/" + texturePath + ".png";
        
        try (InputStream stream = SnifferNoseDefinitionLoader.class.getClassLoader().getResourceAsStream(fullPath)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Load sniffer nose definitions from a specific JSON resource file
     */
    private static SnifferNoseDefinition[] loadSnifferNosesFromResource(String resourcePath) {
        try (InputStream inputStream = SnifferNoseDefinitionLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                AromaCraft.LOGGER.warn("Sniffer noses definitions file not found: {}", resourcePath);
                return new SnifferNoseDefinition[0];
            }
            
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                
                // Support both array format and object with "sniffer_noses" array
                if (jsonElement.isJsonArray()) {
                    return GSON.fromJson(jsonElement, SnifferNoseDefinition[].class);
                } else if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("sniffer_noses")) {
                        return GSON.fromJson(jsonObject.get("sniffer_noses"), SnifferNoseDefinition[].class);
                    }
                }
                
                AromaCraft.LOGGER.warn("Invalid JSON format in: {} (expected array or object with 'sniffer_noses' key)", resourcePath);
                return new SnifferNoseDefinition[0];
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Error parsing sniffer nose definitions from: {}", resourcePath, e);
            return new SnifferNoseDefinition[0];
        }
    }
    
    /**
     * Get a sniffer nose definition by ID
     */
    public static SnifferNoseDefinition getSnifferNoseById(String id) {
        for (SnifferNoseDefinition snifferNose : loadedSnifferNoses) {
            if (snifferNose.getId().equals(id)) {
                return snifferNose;
            }
        }
        return null;
    }
    
    /**
     * Check if a sniffer nose with the given ID has been loaded
     */
    public static boolean hasSnifferNoseId(String id) {
        return loadedIds.contains(id);
    }
    
    /**
     * Serialize a sniffer nose definition to JSON
     */
    public static String toJson(SnifferNoseDefinition snifferNose) {
        return GSON.toJson(snifferNose);
    }
    
    /**
     * Get the GSON instance for external use
     */
    public static Gson getGson() {
        return GSON;
    }
}
