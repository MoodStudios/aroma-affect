package com.ovrtechnology.block;

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
 * Loads trackable block definitions from JSON files.
 * 
 * <p>This loader handles parsing the block configuration from
 * {@code data/aromacraft/blocks/blocks.json} and provides validation
 * for duplicate IDs, HTML color format, and scent references.</p>
 * 
 * <p>The JSON format supports both array format and object with "blocks" array:</p>
 * <pre>
 * // Array format:
 * [
 *   { "block_id": "minecraft:diamond_ore", "color_html": "#5DECF5", "scent_id": "terra_silva" }
 * ]
 * 
 * // Object format:
 * {
 *   "blocks": [
 *     { "block_id": "minecraft:diamond_ore", "color_html": "#5DECF5", "scent_id": "terra_silva" }
 *   ]
 * }
 * </pre>
 * 
 * <h2>Validation</h2>
 * <ul>
 *   <li>Duplicate block_id detection (first occurrence kept)</li>
 *   <li>HTML color format validation (#RGB, #RRGGBB, #RRGGBBAA)</li>
 *   <li>Scent ID validation against ScentRegistry</li>
 * </ul>
 */
public class BlockDefinitionLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Path to the blocks JSON file
     */
    private static final String BLOCKS_RESOURCE_PATH = "data/aromacraft/blocks/blocks.json";
    
    /**
     * Cached list of loaded block definitions
     */
    @Getter
    private static List<BlockDefinition> loadedBlocks = new ArrayList<>();
    
    /**
     * Set of loaded block IDs for duplicate detection
     */
    private static Set<String> loadedIds = new HashSet<>();
    
    /**
     * List of validation warnings encountered during loading
     */
    @Getter
    private static List<String> validationWarnings = new ArrayList<>();
    
    /**
     * Load all block definitions from the blocks directory.
     * This parses the JSON file and validates each entry.
     * 
     * <p>Validation includes:</p>
     * <ul>
     *   <li>Duplicate block_id detection</li>
     *   <li>HTML color format validation</li>
     *   <li>Scent ID reference validation (requires ScentRegistry to be initialized)</li>
     * </ul>
     * 
     * @return An unmodifiable list of valid block definitions
     */
    public static List<BlockDefinition> loadAllBlocks() {
        loadedBlocks.clear();
        loadedIds.clear();
        validationWarnings.clear();
        
        try {
            BlockDefinition[] blocks = loadBlocksFromResource(BLOCKS_RESOURCE_PATH);
            if (blocks != null) {
                for (BlockDefinition block : blocks) {
                    processBlock(block);
                }
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Failed to load block definitions", e);
        }
        
        AromaCraft.LOGGER.info("Loaded {} block definitions", loadedBlocks.size());
        
        if (!validationWarnings.isEmpty()) {
            AromaCraft.LOGGER.warn("Block loading completed with {} validation warnings", validationWarnings.size());
        }
        
        return Collections.unmodifiableList(loadedBlocks);
    }
    
    /**
     * Process a single block definition, validating and adding to the loaded list.
     * 
     * @param block The block definition to process
     */
    private static void processBlock(BlockDefinition block) {
        if (block == null) {
            addWarning("Null block definition found, skipping...");
            return;
        }
        
        if (!block.isValid()) {
            addWarning("Invalid block definition found (missing block_id), skipping...");
            return;
        }
        
        String blockId = block.getBlockId();
        
        // Check for duplicate IDs
        if (loadedIds.contains(blockId)) {
            addWarning("Duplicate block_id '" + blockId + "' found, skipping...");
            return;
        }
        
        // Validate the block entry
        validateBlock(block);
        
        loadedIds.add(blockId);
        loadedBlocks.add(block);
        AromaCraft.LOGGER.debug("Loaded block definition: {} (color: {}, scent: {})", 
                blockId, block.getColorHtml(), block.getScentId());
    }
    
    /**
     * Validate a block definition and log any warnings.
     * 
     * @param block The block to validate
     */
    private static void validateBlock(BlockDefinition block) {
        String blockId = block.getBlockId();
        
        // Validate HTML color format
        String rawColor = block.getRawColorHtml();
        if (rawColor == null || rawColor.isEmpty()) {
            addWarning("[" + blockId + "] No color_html defined, using default: " + BlockDefinition.DEFAULT_COLOR);
        } else if (!BlockDefinition.isValidHtmlColor(rawColor)) {
            addWarning("[" + blockId + "] Invalid color_html format '" + rawColor + "', using default: " + BlockDefinition.DEFAULT_COLOR);
        }
        
        // Validate scent_id reference
        if (block.hasScentId()) {
            String scentId = block.getScentId();
            if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(scentId)) {
                addWarning("[" + blockId + "] Referenced scent_id '" + scentId + "' does not exist in ScentRegistry");
            }
        } else {
            addWarning("[" + blockId + "] No scent_id defined, block will have no associated scent");
        }
        
        // Validate block_id format (should be namespace:path)
        if (!blockId.contains(":")) {
            addWarning("[" + blockId + "] Block ID should include namespace (e.g., 'minecraft:stone')");
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
     * Load block definitions from a specific JSON resource file.
     * 
     * @param resourcePath Path to the JSON resource
     * @return Array of block definitions, or empty array if not found
     */
    private static BlockDefinition[] loadBlocksFromResource(String resourcePath) {
        try (InputStream inputStream = BlockDefinitionLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                AromaCraft.LOGGER.warn("Block definitions file not found: {}", resourcePath);
                return new BlockDefinition[0];
            }
            
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                
                // Support both array format and object with "blocks" array
                if (jsonElement.isJsonArray()) {
                    return GSON.fromJson(jsonElement, BlockDefinition[].class);
                } else if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("blocks")) {
                        return GSON.fromJson(jsonObject.get("blocks"), BlockDefinition[].class);
                    }
                }
                
                AromaCraft.LOGGER.warn("Invalid JSON format in: {} (expected array or object with 'blocks' key)", resourcePath);
                return new BlockDefinition[0];
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Error parsing block definitions from: {}", resourcePath, e);
            return new BlockDefinition[0];
        }
    }
    
    /**
     * Parse a single block definition from a JSON string.
     * 
     * @param json The JSON string to parse
     * @return The parsed block definition, or null if parsing fails
     */
    public static BlockDefinition parseBlockFromJson(String json) {
        try {
            return GSON.fromJson(json, BlockDefinition.class);
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Failed to parse block definition from JSON", e);
            return null;
        }
    }
    
    /**
     * Get a block definition by block ID from the loaded blocks.
     * 
     * @param blockId The block ID to look up
     * @return The block definition, or null if not found
     */
    public static BlockDefinition getBlockById(String blockId) {
        for (BlockDefinition block : loadedBlocks) {
            if (block.getBlockId().equals(blockId)) {
                return block;
            }
        }
        return null;
    }
    
    /**
     * Check if a block with the given ID has been loaded.
     * 
     * @param blockId The block ID to check
     * @return true if the block exists
     */
    public static boolean hasBlockId(String blockId) {
        return loadedIds.contains(blockId);
    }
    
    /**
     * Get all blocks that reference a specific scent.
     * 
     * @param scentId The scent ID to filter by
     * @return List of blocks using the specified scent
     */
    public static List<BlockDefinition> getBlocksByScentId(String scentId) {
        List<BlockDefinition> result = new ArrayList<>();
        for (BlockDefinition block : loadedBlocks) {
            if (scentId.equals(block.getScentId())) {
                result.add(block);
            }
        }
        return result;
    }
    
    /**
     * Serialize a block definition to JSON.
     * 
     * @param block The block to serialize
     * @return JSON string representation
     */
    public static String toJson(BlockDefinition block) {
        return GSON.toJson(block);
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
     * Reload all block definitions.
     * This clears the cache and reloads from the JSON file.
     * 
     * @return The reloaded list of block definitions
     */
    public static List<BlockDefinition> reload() {
        AromaCraft.LOGGER.info("Reloading block definitions...");
        return loadAllBlocks();
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

