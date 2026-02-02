package com.ovrtechnology.nose;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.ability.AbilityDefinitionLoader;
import com.ovrtechnology.biome.BiomeRegistry;
import com.ovrtechnology.block.BlockRegistry;
import com.ovrtechnology.structure.StructureRegistry;
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
 * Parses the data/aromaaffect/noses/ directory for nose definitions.
 * Handles texture validation and fallback to basic_nose texture.
 * 
 * Note: Recipes are defined separately in data/aromaaffect/recipe/ as standard Minecraft recipe JSON files.
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
            NoseDefinition[] noses = loadNosesFromResource("data/aromaaffect/noses/noses.json");
            if (noses != null) {
                for (NoseDefinition nose : noses) {
                    if (nose != null && nose.isValid()) {
                        // Validate and apply fallbacks
                        validateAndApplyFallbacks(nose);
                        
                        loadedNoses.add(nose);
                        AromaAffect.LOGGER.info("Loaded nose definition: {}", nose.getId());
                    } else {
                        AromaAffect.LOGGER.warn("Invalid nose definition found, skipping...");
                    }
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load nose definitions", e);
        }
        
        AromaAffect.LOGGER.info("Loaded {} nose definitions", loadedNoses.size());
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
            AromaAffect.LOGGER.warn("[{}] No texture defined, using fallback: {}", noseId, DEFAULT_TEXTURE);
            nose.setImage(DEFAULT_TEXTURE);
        } else if (!textureExists(texturePath)) {
            AromaAffect.LOGGER.warn("[{}] Texture '{}' not found, using fallback: {}", noseId, texturePath, DEFAULT_TEXTURE);
            nose.setImage(DEFAULT_TEXTURE);
        }
        
        // Check model - default to iron_helmet if not specified
        String model = nose.getModel();
        if (model == null || model.isEmpty()) {
            AromaAffect.LOGGER.info("[{}] No model defined, using default: {}", noseId, DEFAULT_MODEL);
        }
        
        // Validate unlock references
        NoseUnlock unlock = nose.getUnlock();
        if (unlock != null) {
            // Validate nose inheritance
            if (unlock.hasNoseInheritance()) {
                for (String inheritedNoseId : unlock.getNoses()) {
                    AromaAffect.LOGGER.debug("[{}] Inherits abilities from: {}", noseId, inheritedNoseId);
                }
            }
            
            // Validate and filter block references against BlockRegistry
            if (unlock.hasBlockUnlocks() && BlockRegistry.isInitialized()) {
                List<String> invalidBlocks = BlockRegistry.validateBlockIds(unlock.getBlocks());
                if (!invalidBlocks.isEmpty()) {
                    for (String invalidBlock : invalidBlocks) {
                        AromaAffect.LOGGER.warn("[{}] Skipping unregistered block: '{}' - block must be defined in blocks.json", 
                                noseId, invalidBlock);
                    }
                    // Filter out invalid blocks
                    List<String> validBlocks = new ArrayList<>(unlock.getBlocks());
                    validBlocks.removeAll(invalidBlocks);
                    unlock.setBlocks(validBlocks);
                    AromaAffect.LOGGER.info("[{}] Kept {} valid block references after filtering", noseId, validBlocks.size());
                }
            }
            
            // Validate and filter structure references against StructureRegistry
            if (unlock.hasStructureUnlocks() && StructureRegistry.isInitialized()) {
                List<String> invalidStructures = StructureRegistry.validateStructureIds(unlock.getStructures());
                if (!invalidStructures.isEmpty()) {
                    for (String invalidStructure : invalidStructures) {
                        AromaAffect.LOGGER.warn("[{}] Skipping unregistered structure: '{}' - structure must be defined in structures.json", 
                                noseId, invalidStructure);
                    }
                    // Filter out invalid structures
                    List<String> validStructures = new ArrayList<>(unlock.getStructures());
                    validStructures.removeAll(invalidStructures);
                    unlock.setStructures(validStructures);
                    AromaAffect.LOGGER.info("[{}] Kept {} valid structure references after filtering", noseId, validStructures.size());
                }
            }
            
            // Validate and filter biome references against BiomeRegistry
            if (unlock.hasBiomeUnlocks() && BiomeRegistry.isInitialized()) {
                List<String> invalidBiomes = BiomeRegistry.validateBiomeIds(unlock.getBiomes());
                if (!invalidBiomes.isEmpty()) {
                    for (String invalidBiome : invalidBiomes) {
                        AromaAffect.LOGGER.warn("[{}] Skipping unregistered biome: '{}' - biome must be defined in biomes.json", 
                                noseId, invalidBiome);
                    }
                    // Filter out invalid biomes
                    List<String> validBiomes = new ArrayList<>(unlock.getBiomes());
                    validBiomes.removeAll(invalidBiomes);
                    unlock.setBiomes(validBiomes);
                    AromaAffect.LOGGER.info("[{}] Kept {} valid biome references after filtering", noseId, validBiomes.size());
                }
            }
            
            // Validate and filter ability references against AbilityDefinitionLoader
            if (unlock.hasAbilityUnlocks() && AbilityDefinitionLoader.isInitialized()) {
                List<String> invalidAbilities = AbilityDefinitionLoader.validateAbilityIds(unlock.getAbilities());
                if (!invalidAbilities.isEmpty()) {
                    for (String invalidAbility : invalidAbilities) {
                        AromaAffect.LOGGER.warn("[{}] Skipping unregistered ability: '{}' - ability must be defined in abilities.json", 
                                noseId, invalidAbility);
                    }
                    // Filter out invalid abilities
                    List<String> validAbilities = new ArrayList<>(unlock.getAbilities());
                    validAbilities.removeAll(invalidAbilities);
                    unlock.setAbilities(validAbilities);
                    AromaAffect.LOGGER.info("[{}] Kept {} valid ability references after filtering", noseId, validAbilities.size());
                }
            }
        }
    }
    
    /**
     * Check if a texture file exists in the resources
     */
    private static boolean textureExists(String texturePath) {
        // Convert texture path to full resource path
        // texturePath is like "item/basic_nose", full path is "assets/aromaaffect/textures/item/basic_nose.png"
        String fullPath = "assets/aromaaffect/textures/" + texturePath + ".png";
        
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
                AromaAffect.LOGGER.warn("Nose definitions file not found: {}", resourcePath);
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
                
                AromaAffect.LOGGER.warn("Invalid JSON format in: {}", resourcePath);
                return new NoseDefinition[0];
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error parsing nose definitions from: {}", resourcePath, e);
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
            AromaAffect.LOGGER.error("Failed to parse nose definition from JSON", e);
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
