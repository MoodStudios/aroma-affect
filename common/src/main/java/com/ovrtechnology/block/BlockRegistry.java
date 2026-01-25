package com.ovrtechnology.block;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import lombok.Getter;

import java.util.*;

/**
 * Central registry for all trackable block definitions in Aroma Affect.
 * 
 * <p>This class provides the main API for other systems to access block information.
 * Blocks define which Minecraft blocks can be tracked by the Nose system, along with
 * their associated scents and display colors.</p>
 * 
 * <h2>Initialization Order</h2>
 * <p>BlockRegistry must be initialized <strong>after</strong> ScentRegistry,
 * as it validates scent references during loading.</p>
 * 
 * <h2>Usage Example</h2>
 * <pre>
 * // Get a block by ID
 * Optional&lt;BlockDefinition&gt; block = BlockRegistry.getBlock("minecraft:diamond_ore");
 * 
 * // Check if a block is trackable
 * boolean canTrack = BlockRegistry.hasBlock("minecraft:gold_ore");
 * 
 * // Get the scent for a block
 * Optional&lt;ScentDefinition&gt; scent = BlockRegistry.getScentForBlock("minecraft:lava");
 * 
 * // Get all blocks using a specific scent
 * List&lt;BlockDefinition&gt; fireBlocks = BlockRegistry.getBlocksByScent("smoky");
 * </pre>
 */
public final class BlockRegistry {
    
    /**
     * Map of block ID to its definition
     */
    @Getter
    private static final Map<String, BlockDefinition> blockDefinitions = new LinkedHashMap<>();
    
    /**
     * Whether the registry has been initialized
     */
    @Getter
    private static boolean initialized = false;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private BlockRegistry() {
        throw new UnsupportedOperationException("BlockRegistry is a static utility class");
    }
    
    /**
     * Initialize the block registry.
     * This loads block definitions from JSON.
     * Must be called during mod initialization, after ScentRegistry.init().
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("BlockRegistry.init() called multiple times!");
            return;
        }
        
        // Warn if ScentRegistry is not initialized
        if (!ScentRegistry.isInitialized()) {
            AromaAffect.LOGGER.warn("BlockRegistry.init() called before ScentRegistry! Scent validation may fail.");
        }
        
        AromaAffect.LOGGER.info("Initializing BlockRegistry...");
        
        // Load block definitions from JSON
        List<BlockDefinition> definitions = BlockDefinitionLoader.loadAllBlocks();
        
        // Store each block in the map
        for (BlockDefinition definition : definitions) {
            registerBlock(definition);
        }
        
        initialized = true;
        AromaAffect.LOGGER.info("BlockRegistry initialized with {} blocks", blockDefinitions.size());
    }
    
    /**
     * Register a single block from its definition.
     * 
     * @param definition The block definition to register
     */
    private static void registerBlock(BlockDefinition definition) {
        String blockId = definition.getBlockId();
        
        if (blockDefinitions.containsKey(blockId)) {
            AromaAffect.LOGGER.warn("Duplicate block ID in registry: {}, skipping...", blockId);
            return;
        }
        
        blockDefinitions.put(blockId, definition);
        AromaAffect.LOGGER.debug("Registered block: {}", blockId);
    }
    
    /**
     * Get a block definition by block ID.
     * 
     * @param blockId The Minecraft block ID (e.g., "minecraft:diamond_ore")
     * @return Optional containing the block if found
     */
    public static Optional<BlockDefinition> getBlock(String blockId) {
        return Optional.ofNullable(blockDefinitions.get(blockId));
    }
    
    /**
     * Get a block definition by ID, or throw if not found.
     * 
     * @param blockId The block ID
     * @return The block definition
     * @throws IllegalArgumentException if block not found
     */
    public static BlockDefinition getBlockOrThrow(String blockId) {
        BlockDefinition block = blockDefinitions.get(blockId);
        if (block == null) {
            throw new IllegalArgumentException("Unknown block ID: " + blockId);
        }
        return block;
    }
    
    /**
     * Check if a block with the given ID is registered.
     * 
     * @param blockId The block ID to check
     * @return true if the block is trackable
     */
    public static boolean hasBlock(String blockId) {
        return blockDefinitions.containsKey(blockId);
    }
    
    /**
     * Get all registered block IDs.
     * 
     * @return Iterable of all block IDs
     */
    public static Iterable<String> getAllBlockIds() {
        return Collections.unmodifiableSet(blockDefinitions.keySet());
    }
    
    /**
     * Get all registered block definitions.
     * 
     * @return Iterable of all block definitions
     */
    public static Iterable<BlockDefinition> getAllBlocks() {
        return Collections.unmodifiableCollection(blockDefinitions.values());
    }
    
    /**
     * Get all block definitions as a list.
     * 
     * @return List of all block definitions
     */
    public static List<BlockDefinition> getAllBlocksAsList() {
        return new ArrayList<>(blockDefinitions.values());
    }
    
    /**
     * Get the number of registered blocks.
     * 
     * @return The block count
     */
    public static int getBlockCount() {
        return blockDefinitions.size();
    }
    
    /**
     * Get the scent definition associated with a block.
     * 
     * @param blockId The block ID
     * @return Optional containing the scent if block exists and has a scent
     */
    public static Optional<ScentDefinition> getScentForBlock(String blockId) {
        BlockDefinition block = blockDefinitions.get(blockId);
        if (block != null && block.hasScentId()) {
            return ScentRegistry.getScent(block.getScentId());
        }
        return Optional.empty();
    }
    
    /**
     * Get all blocks that use a specific scent.
     * 
     * @param scentId The scent ID to filter by
     * @return List of blocks using the specified scent
     */
    public static List<BlockDefinition> getBlocksByScent(String scentId) {
        List<BlockDefinition> result = new ArrayList<>();
        for (BlockDefinition block : blockDefinitions.values()) {
            if (scentId.equals(block.getScentId())) {
                result.add(block);
            }
        }
        return result;
    }
    
    /**
     * Get the display color for a block as an integer.
     * 
     * @param blockId The block ID
     * @return RGB color as integer, or 0xFFFFFF (white) if not found
     */
    public static int getBlockColor(String blockId) {
        BlockDefinition block = blockDefinitions.get(blockId);
        return block != null ? block.getColorAsInt() : 0xFFFFFF;
    }
    
    /**
     * Get the display color for a block as HTML hex string.
     * 
     * @param blockId The block ID
     * @return HTML color string, or "#FFFFFF" if not found
     */
    public static String getBlockColorHtml(String blockId) {
        BlockDefinition block = blockDefinitions.get(blockId);
        return block != null ? block.getColorHtml() : BlockDefinition.DEFAULT_COLOR;
    }
    
    /**
     * Validate that all block IDs in a list are registered.
     * Useful for validating nose definitions that reference blocks.
     * 
     * @param blockIds List of block IDs to validate
     * @return List of invalid (unregistered) block IDs
     */
    public static List<String> validateBlockIds(List<String> blockIds) {
        List<String> invalid = new ArrayList<>();
        for (String id : blockIds) {
            if (!hasBlock(id)) {
                invalid.add(id);
            }
        }
        return invalid;
    }
    
    /**
     * Reload all block definitions from JSON.
     * This clears the registry and reloads from the configuration file.
     */
    public static void reload() {
        AromaAffect.LOGGER.info("Reloading BlockRegistry...");
        blockDefinitions.clear();
        
        List<BlockDefinition> definitions = BlockDefinitionLoader.reload();
        for (BlockDefinition definition : definitions) {
            registerBlock(definition);
        }
        
        AromaAffect.LOGGER.info("BlockRegistry reloaded with {} blocks", blockDefinitions.size());
    }
    
    /**
     * Clear the registry (primarily for testing).
     */
    static void clear() {
        blockDefinitions.clear();
        initialized = false;
    }
}

