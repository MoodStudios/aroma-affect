package com.ovrtechnology.nose;

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
import java.util.List;

/**
 * Loads nose definitions from JSON files.
 * Parses the data/aromacraft/noses/ directory for nose definitions.
 * Handles texture validation and fallback to basic_nose texture.
 * 
 * Note: Recipes are defined separately in data/aromacraft/recipe/ as standard Minecraft recipe JSON files.
 */
public class NoseDefinitionLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Default texture path for fallback
     */
    private static final String DEFAULT_TEXTURE = "item/basic_nose";
    
    /**
     * Default model (iron helmet) for fallback
     */
    private static final String DEFAULT_MODEL = "minecraft:iron_helmet";
    
    /**
     * Cached list of loaded nose definitions
     */
    @Getter
    private static List<NoseDefinition> loadedNoses = new ArrayList<>();
    
    /**
     * Load all nose definitions from the noses directory.
     * This scans for JSON files and parses each one.
     */
    public static List<NoseDefinition> loadAllNoses() {
        loadedNoses.clear();
        
        // Load the index file that lists all nose definition files
        try {
            NoseDefinition[] noses = loadNosesFromResource("data/aromacraft/noses/noses.json");
            if (noses != null) {
                for (NoseDefinition nose : noses) {
                    if (nose != null && nose.isValid()) {
                        // Validate and apply fallbacks
                        validateAndApplyFallbacks(nose);
                        
                        loadedNoses.add(nose);
                        AromaCraft.LOGGER.info("Loaded nose definition: {}", nose.getId());
                    } else {
                        AromaCraft.LOGGER.warn("Invalid nose definition found, skipping...");
                    }
                }
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Failed to load nose definitions", e);
        }
        
        AromaCraft.LOGGER.info("Loaded {} nose definitions", loadedNoses.size());
        return Collections.unmodifiableList(loadedNoses);
    }
    
    /**
     * Validate nose definition and apply fallbacks for missing textures/models
     */
    private static void validateAndApplyFallbacks(NoseDefinition nose) {
        String noseId = nose.getId();
        
        // Check texture
        String texturePath = nose.getImage();
        if (texturePath == null || texturePath.isEmpty()) {
            AromaCraft.LOGGER.warn("[{}] No texture defined, using fallback: {}", noseId, DEFAULT_TEXTURE);
            nose.setImage(DEFAULT_TEXTURE);
        } else if (!textureExists(texturePath)) {
            AromaCraft.LOGGER.warn("[{}] Texture '{}' not found, using fallback: {}", noseId, texturePath, DEFAULT_TEXTURE);
            nose.setImage(DEFAULT_TEXTURE);
        }
        
        // Check model - default to iron_helmet if not specified
        String model = nose.getModel();
        if (model == null || model.isEmpty()) {
            AromaCraft.LOGGER.info("[{}] No model defined, using default: {}", noseId, DEFAULT_MODEL);
        }
        
        // Validate unlock references
        NoseUnlock unlock = nose.getUnlock();
        if (unlock != null && unlock.hasNoseInheritance()) {
            for (String inheritedNoseId : unlock.getNoses()) {
                AromaCraft.LOGGER.debug("[{}] Inherits abilities from: {}", noseId, inheritedNoseId);
            }
        }
    }
    
    /**
     * Check if a texture file exists in the resources
     */
    private static boolean textureExists(String texturePath) {
        // Convert texture path to full resource path
        // texturePath is like "item/basic_nose", full path is "assets/aromacraft/textures/item/basic_nose.png"
        String fullPath = "assets/aromacraft/textures/" + texturePath + ".png";
        
        try (InputStream stream = NoseDefinitionLoader.class.getClassLoader().getResourceAsStream(fullPath)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Load nose definitions from a specific JSON resource file
     */
    private static NoseDefinition[] loadNosesFromResource(String resourcePath) {
        try (InputStream inputStream = NoseDefinitionLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                AromaCraft.LOGGER.warn("Nose definitions file not found: {}", resourcePath);
                return new NoseDefinition[0];
            }
            
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                
                // Support both array format and object with "noses" array
                if (jsonElement.isJsonArray()) {
                    return GSON.fromJson(jsonElement, NoseDefinition[].class);
                } else if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("noses")) {
                        return GSON.fromJson(jsonObject.get("noses"), NoseDefinition[].class);
                    }
                }
                
                AromaCraft.LOGGER.warn("Invalid JSON format in: {}", resourcePath);
                return new NoseDefinition[0];
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Error parsing nose definitions from: {}", resourcePath, e);
            return new NoseDefinition[0];
        }
    }
    
    /**
     * Parse a single nose definition from a JSON string
     */
    public static NoseDefinition parseNoseFromJson(String json) {
        try {
            return GSON.fromJson(json, NoseDefinition.class);
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Failed to parse nose definition from JSON", e);
            return null;
        }
    }
    
    /**
     * Get a nose definition by ID
     */
    public static NoseDefinition getNoseById(String id) {
        for (NoseDefinition nose : loadedNoses) {
            if (nose.getId().equals(id)) {
                return nose;
            }
        }
        return null;
    }
    
    /**
     * Get all nose definitions for a specific tier
     */
    public static List<NoseDefinition> getNosesByTier(int tier) {
        List<NoseDefinition> result = new ArrayList<>();
        for (NoseDefinition nose : loadedNoses) {
            if (nose.getTier() == tier) {
                result.add(nose);
            }
        }
        return result;
    }
    
    /**
     * Serialize a nose definition to JSON (useful for debugging/export)
     */
    public static String toJson(NoseDefinition nose) {
        return GSON.toJson(nose);
    }
    
    /**
     * Get the GSON instance for external use
     */
    public static Gson getGson() {
        return GSON;
    }
}
