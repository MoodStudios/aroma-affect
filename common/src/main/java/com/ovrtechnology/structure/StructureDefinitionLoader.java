package com.ovrtechnology.structure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.block.BlockRegistry;
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
 * Loads trackable structure definitions from JSON files.
 * 
 * <p>This loader handles parsing the structure configuration from
 * {@code data/aromaaffect/structures/structures.json} and provides validation
 * for duplicate IDs, HTML color format, scent references, and block references.</p>
 * 
 * <p>The JSON format supports both array format and object with "structures" array:</p>
 * <pre>
 * // Array format:
 * [
 *   { "structure_id": "minecraft:stronghold", "color_html": "#4A4A4A", "scent_id": "terra_silva" }
 * ]
 * 
 * // Object format:
 * {
 *   "structures": [
 *     { "structure_id": "minecraft:stronghold", "color_html": "#4A4A4A", "scent_id": "terra_silva" }
 *   ]
 * }
 * </pre>
 * 
 * <h2>Validation</h2>
 * <ul>
 *   <li>Duplicate structure_id detection (first occurrence kept)</li>
 *   <li>Structure ID format validation (namespace:path)</li>
 *   <li>HTML color format validation (#RGB, #RRGGBB, #RRGGBBAA)</li>
 *   <li>Scent ID validation against ScentRegistry</li>
 *   <li>Block IDs validation against BlockRegistry (warnings only)</li>
 * </ul>
 */
public class StructureDefinitionLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Path to the structures JSON file
     */
    private static final String STRUCTURES_RESOURCE_PATH = "data/aromaaffect/structures/structures.json";
    
    /**
     * Cached list of loaded structure definitions
     */
    @Getter
    private static List<StructureDefinition> loadedStructures = new ArrayList<>();
    
    /**
     * Set of loaded structure IDs for duplicate detection
     */
    private static Set<String> loadedIds = new HashSet<>();
    
    /**
     * List of validation warnings encountered during loading
     */
    @Getter
    private static List<String> validationWarnings = new ArrayList<>();
    
    /**
     * Load all structure definitions from the structures directory.
     * This parses the JSON file and validates each entry.
     * 
     * <p>Validation includes:</p>
     * <ul>
     *   <li>Duplicate structure_id detection</li>
     *   <li>Structure ID format (namespace:path)</li>
     *   <li>HTML color format validation</li>
     *   <li>Scent ID reference validation</li>
     *   <li>Block IDs reference validation</li>
     * </ul>
     * 
     * @return An unmodifiable list of valid structure definitions
     */
    public static List<StructureDefinition> loadAllStructures() {
        loadedStructures.clear();
        loadedIds.clear();
        validationWarnings.clear();
        
        try {
            StructureDefinition[] structures = loadStructuresFromResource(STRUCTURES_RESOURCE_PATH);
            if (structures != null) {
                for (StructureDefinition structure : structures) {
                    processStructure(structure);
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load structure definitions", e);
        }
        
        AromaAffect.LOGGER.info("Loaded {} structure definitions", loadedStructures.size());
        
        if (!validationWarnings.isEmpty()) {
            AromaAffect.LOGGER.warn("Structure loading completed with {} validation warnings", validationWarnings.size());
        }
        
        return Collections.unmodifiableList(loadedStructures);
    }
    
    /**
     * Process a single structure definition, validating and adding to the loaded list.
     * 
     * @param structure The structure definition to process
     */
    private static void processStructure(StructureDefinition structure) {
        if (structure == null) {
            addWarning("Null structure definition found, skipping...");
            return;
        }
        
        if (!structure.isValid()) {
            addWarning("Invalid structure definition found (missing structure_id), skipping...");
            return;
        }
        
        String structureId = structure.getStructureId();
        
        // Check for duplicate IDs
        if (loadedIds.contains(structureId)) {
            addWarning("Duplicate structure_id '" + structureId + "' found, skipping...");
            return;
        }
        
        // Validate the structure entry
        validateStructure(structure);
        
        loadedIds.add(structureId);
        loadedStructures.add(structure);
        AromaAffect.LOGGER.debug("Loaded structure definition: {} (color: {}, scent: {})", 
                structureId, structure.getColorHtml(), structure.getScentId());
    }
    
    /**
     * Validate a structure definition and log any warnings.
     * 
     * @param structure The structure to validate
     */
    private static void validateStructure(StructureDefinition structure) {
        String structureId = structure.getStructureId();
        
        // Validate structure_id format (namespace:path)
        if (!structure.hasValidStructureIdFormat()) {
            addWarning("[" + structureId + "] Invalid structure_id format - should be 'namespace:path' (e.g., 'minecraft:stronghold')");
        }
        
        // Validate HTML color format
        String rawColor = structure.getRawColorHtml();
        if (rawColor == null || rawColor.isEmpty()) {
            addWarning("[" + structureId + "] No color_html defined, using default: " + StructureDefinition.DEFAULT_COLOR);
        } else if (!StructureDefinition.isValidHtmlColor(rawColor)) {
            addWarning("[" + structureId + "] Invalid color_html format '" + rawColor + "', using default: " + StructureDefinition.DEFAULT_COLOR);
        }
        
        // Validate scent_id reference
        if (structure.hasScentId()) {
            String scentId = structure.getScentId();
            if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(scentId)) {
                addWarning("[" + structureId + "] Referenced scent_id '" + scentId + "' does not exist in ScentRegistry");
            }
        } else {
            addWarning("[" + structureId + "] No scent_id defined, structure will have no associated scent");
        }
        
        // Validate blocks list
        if (structure.hasBlocks()) {
            validateBlockReferences(structureId, structure.getBlocks());
        }
        
        // Validate image path
        String rawImage = structure.getRawImage();
        if (rawImage == null || rawImage.isEmpty()) {
            AromaAffect.LOGGER.debug("[{}] No image defined, using default: {}", structureId, StructureDefinition.DEFAULT_IMAGE);
        }
    }
    
    /**
     * Validate block references in the structure's blocks list.
     * 
     * @param structureId The structure ID for logging
     * @param blockIds List of block IDs to validate
     */
    private static void validateBlockReferences(String structureId, List<String> blockIds) {
        if (!BlockRegistry.isInitialized()) {
            AromaAffect.LOGGER.debug("[{}] BlockRegistry not initialized, skipping block validation", structureId);
            return;
        }
        
        List<String> invalidBlocks = BlockRegistry.validateBlockIds(blockIds);
        for (String invalidBlock : invalidBlocks) {
            // This is just a warning - blocks in structures don't need to be in our BlockRegistry
            // They just need to be valid Minecraft block IDs
            AromaAffect.LOGGER.debug("[{}] Block '{}' not found in BlockRegistry (may still be valid Minecraft block)", 
                    structureId, invalidBlock);
        }
        
        // Validate block ID format
        for (String blockId : blockIds) {
            if (!StructureDefinition.isValidResourceLocation(blockId)) {
                addWarning("[" + structureId + "] Block ID '" + blockId + "' has invalid format - should be 'namespace:path'");
            }
        }
    }
    
    /**
     * Add a validation warning.
     * 
     * @param warning The warning message
     */
    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaAffect.LOGGER.warn(warning);
    }
    
    /**
     * Load structure definitions from a specific JSON resource file.
     * 
     * @param resourcePath Path to the JSON resource
     * @return Array of structure definitions, or empty array if not found
     */
    private static StructureDefinition[] loadStructuresFromResource(String resourcePath) {
        try (InputStream inputStream = StructureDefinitionLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                AromaAffect.LOGGER.warn("Structure definitions file not found: {}", resourcePath);
                return new StructureDefinition[0];
            }
            
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                
                // Support both array format and object with "structures" array
                if (jsonElement.isJsonArray()) {
                    return GSON.fromJson(jsonElement, StructureDefinition[].class);
                } else if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("structures")) {
                        return GSON.fromJson(jsonObject.get("structures"), StructureDefinition[].class);
                    }
                }
                
                AromaAffect.LOGGER.warn("Invalid JSON format in: {} (expected array or object with 'structures' key)", resourcePath);
                return new StructureDefinition[0];
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error parsing structure definitions from: {}", resourcePath, e);
            return new StructureDefinition[0];
        }
    }
    
    /**
     * Parse a single structure definition from a JSON string.
     * 
     * @param json The JSON string to parse
     * @return The parsed structure definition, or null if parsing fails
     */
    public static StructureDefinition parseStructureFromJson(String json) {
        try {
            return GSON.fromJson(json, StructureDefinition.class);
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to parse structure definition from JSON", e);
            return null;
        }
    }
    
    /**
     * Get a structure definition by structure ID from the loaded structures.
     * 
     * @param structureId The structure ID to look up
     * @return The structure definition, or null if not found
     */
    public static StructureDefinition getStructureById(String structureId) {
        for (StructureDefinition structure : loadedStructures) {
            if (structure.getStructureId().equals(structureId)) {
                return structure;
            }
        }
        return null;
    }
    
    /**
     * Check if a structure with the given ID has been loaded.
     * 
     * @param structureId The structure ID to check
     * @return true if the structure exists
     */
    public static boolean hasStructureId(String structureId) {
        return loadedIds.contains(structureId);
    }
    
    /**
     * Get all structures that reference a specific scent.
     * 
     * @param scentId The scent ID to filter by
     * @return List of structures using the specified scent
     */
    public static List<StructureDefinition> getStructuresByScentId(String scentId) {
        List<StructureDefinition> result = new ArrayList<>();
        for (StructureDefinition structure : loadedStructures) {
            if (scentId.equals(structure.getScentId())) {
                result.add(structure);
            }
        }
        return result;
    }
    
    /**
     * Get all vanilla Minecraft structures.
     * 
     * @return List of structures with "minecraft" namespace
     */
    public static List<StructureDefinition> getVanillaStructures() {
        List<StructureDefinition> result = new ArrayList<>();
        for (StructureDefinition structure : loadedStructures) {
            if (structure.isVanilla()) {
                result.add(structure);
            }
        }
        return result;
    }
    
    /**
     * Get all modded structures (non-vanilla).
     * 
     * @return List of structures without "minecraft" namespace
     */
    public static List<StructureDefinition> getModdedStructures() {
        List<StructureDefinition> result = new ArrayList<>();
        for (StructureDefinition structure : loadedStructures) {
            if (!structure.isVanilla()) {
                result.add(structure);
            }
        }
        return result;
    }
    
    /**
     * Serialize a structure definition to JSON.
     * 
     * @param structure The structure to serialize
     * @return JSON string representation
     */
    public static String toJson(StructureDefinition structure) {
        return GSON.toJson(structure);
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
     * Reload all structure definitions.
     * This clears the cache and reloads from the JSON file.
     * 
     * @return The reloaded list of structure definitions
     */
    public static List<StructureDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading structure definitions...");
        return loadAllStructures();
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

