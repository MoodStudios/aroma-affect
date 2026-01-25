package com.ovrtechnology.structure;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import lombok.Getter;

import java.util.*;

/**
 * Central registry for all trackable structure definitions in Aroma Affect.
 * 
 * <p>This class provides the main API for other systems to access structure information.
 * Structures define which Minecraft structures can be tracked by the Nose system, along with
 * their associated scents, display colors, and characteristic blocks.</p>
 * 
 * <h2>Initialization Order</h2>
 * <p>StructureRegistry must be initialized <strong>after</strong> ScentRegistry and BlockRegistry,
 * as it validates references during loading.</p>
 * 
 * <h2>Usage Example</h2>
 * <pre>
 * // Get a structure by ID
 * Optional&lt;StructureDefinition&gt; structure = StructureRegistry.getStructure("minecraft:stronghold");
 * 
 * // Check if a structure is trackable
 * boolean canTrack = StructureRegistry.hasStructure("minecraft:village_plains");
 * 
 * // Get the scent for a structure
 * Optional&lt;ScentDefinition&gt; scent = StructureRegistry.getScentForStructure("minecraft:ancient_city");
 * 
 * // Get all vanilla structures
 * List&lt;StructureDefinition&gt; vanilla = StructureRegistry.getVanillaStructures();
 * </pre>
 * 
 * <h2>Modded Structure Support</h2>
 * <p>The registry supports structures from any mod. Structure IDs use the standard
 * Minecraft ResourceLocation format (namespace:path), allowing modded structures
 * to be registered alongside vanilla ones.</p>
 */
public final class StructureRegistry {
    
    /**
     * Map of structure ID to its definition
     */
    @Getter
    private static final Map<String, StructureDefinition> structureDefinitions = new LinkedHashMap<>();
    
    /**
     * Whether the registry has been initialized
     */
    @Getter
    private static boolean initialized = false;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private StructureRegistry() {
        throw new UnsupportedOperationException("StructureRegistry is a static utility class");
    }
    
    /**
     * Initialize the structure registry.
     * This loads structure definitions from JSON.
     * Must be called during mod initialization, after ScentRegistry and BlockRegistry.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("StructureRegistry.init() called multiple times!");
            return;
        }
        
        // Warn if dependencies are not initialized
        if (!ScentRegistry.isInitialized()) {
            AromaAffect.LOGGER.warn("StructureRegistry.init() called before ScentRegistry! Scent validation may fail.");
        }
        
        AromaAffect.LOGGER.info("Initializing StructureRegistry...");
        
        // Load structure definitions from JSON
        List<StructureDefinition> definitions = StructureDefinitionLoader.loadAllStructures();
        
        // Store each structure in the map
        for (StructureDefinition definition : definitions) {
            registerStructure(definition);
        }
        
        initialized = true;
        AromaAffect.LOGGER.info("StructureRegistry initialized with {} structures", structureDefinitions.size());
    }
    
    /**
     * Register a single structure from its definition.
     * 
     * @param definition The structure definition to register
     */
    private static void registerStructure(StructureDefinition definition) {
        String structureId = definition.getStructureId();
        
        if (structureDefinitions.containsKey(structureId)) {
            AromaAffect.LOGGER.warn("Duplicate structure ID in registry: {}, skipping...", structureId);
            return;
        }
        
        structureDefinitions.put(structureId, definition);
        AromaAffect.LOGGER.debug("Registered structure: {}", structureId);
    }
    
    /**
     * Get a structure definition by structure ID.
     * 
     * @param structureId The Minecraft structure ID (e.g., "minecraft:stronghold")
     * @return Optional containing the structure if found
     */
    public static Optional<StructureDefinition> getStructure(String structureId) {
        return Optional.ofNullable(structureDefinitions.get(structureId));
    }
    
    /**
     * Get a structure definition by ID, or throw if not found.
     * 
     * @param structureId The structure ID
     * @return The structure definition
     * @throws IllegalArgumentException if structure not found
     */
    public static StructureDefinition getStructureOrThrow(String structureId) {
        StructureDefinition structure = structureDefinitions.get(structureId);
        if (structure == null) {
            throw new IllegalArgumentException("Unknown structure ID: " + structureId);
        }
        return structure;
    }
    
    /**
     * Check if a structure with the given ID is registered.
     * 
     * @param structureId The structure ID to check
     * @return true if the structure is trackable
     */
    public static boolean hasStructure(String structureId) {
        return structureDefinitions.containsKey(structureId);
    }
    
    /**
     * Get all registered structure IDs.
     * 
     * @return Iterable of all structure IDs
     */
    public static Iterable<String> getAllStructureIds() {
        return Collections.unmodifiableSet(structureDefinitions.keySet());
    }
    
    /**
     * Get all registered structure definitions.
     * 
     * @return Iterable of all structure definitions
     */
    public static Iterable<StructureDefinition> getAllStructures() {
        return Collections.unmodifiableCollection(structureDefinitions.values());
    }
    
    /**
     * Get all structure definitions as a list.
     * 
     * @return List of all structure definitions
     */
    public static List<StructureDefinition> getAllStructuresAsList() {
        return new ArrayList<>(structureDefinitions.values());
    }
    
    /**
     * Get the number of registered structures.
     * 
     * @return The structure count
     */
    public static int getStructureCount() {
        return structureDefinitions.size();
    }
    
    /**
     * Get the scent definition associated with a structure.
     * 
     * @param structureId The structure ID
     * @return Optional containing the scent if structure exists and has a scent
     */
    public static Optional<ScentDefinition> getScentForStructure(String structureId) {
        StructureDefinition structure = structureDefinitions.get(structureId);
        if (structure != null && structure.hasScentId()) {
            return ScentRegistry.getScent(structure.getScentId());
        }
        return Optional.empty();
    }
    
    /**
     * Get all structures that use a specific scent.
     * 
     * @param scentId The scent ID to filter by
     * @return List of structures using the specified scent
     */
    public static List<StructureDefinition> getStructuresByScent(String scentId) {
        List<StructureDefinition> result = new ArrayList<>();
        for (StructureDefinition structure : structureDefinitions.values()) {
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
        for (StructureDefinition structure : structureDefinitions.values()) {
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
        for (StructureDefinition structure : structureDefinitions.values()) {
            if (!structure.isVanilla()) {
                result.add(structure);
            }
        }
        return result;
    }
    
    /**
     * Get structures by namespace (e.g., "minecraft", "create", "mekanism").
     * 
     * @param namespace The mod namespace to filter by
     * @return List of structures from that namespace
     */
    public static List<StructureDefinition> getStructuresByNamespace(String namespace) {
        List<StructureDefinition> result = new ArrayList<>();
        for (StructureDefinition structure : structureDefinitions.values()) {
            if (namespace.equals(structure.getNamespace())) {
                result.add(structure);
            }
        }
        return result;
    }
    
    /**
     * Get the display color for a structure as an integer.
     * 
     * @param structureId The structure ID
     * @return RGB color as integer, or 0x808080 (gray) if not found
     */
    public static int getStructureColor(String structureId) {
        StructureDefinition structure = structureDefinitions.get(structureId);
        return structure != null ? structure.getColorAsInt() : 0x808080;
    }
    
    /**
     * Get the display color for a structure as HTML hex string.
     * 
     * @param structureId The structure ID
     * @return HTML color string, or "#808080" if not found
     */
    public static String getStructureColorHtml(String structureId) {
        StructureDefinition structure = structureDefinitions.get(structureId);
        return structure != null ? structure.getColorHtml() : StructureDefinition.DEFAULT_COLOR;
    }
    
    /**
     * Validate that all structure IDs in a list are registered.
     * Useful for validating nose definitions that reference structures.
     * 
     * @param structureIds List of structure IDs to validate
     * @return List of invalid (unregistered) structure IDs
     */
    public static List<String> validateStructureIds(List<String> structureIds) {
        List<String> invalid = new ArrayList<>();
        for (String id : structureIds) {
            if (!hasStructure(id)) {
                invalid.add(id);
            }
        }
        return invalid;
    }
    
    /**
     * Get all unique namespaces from registered structures.
     * 
     * @return Set of namespace strings (e.g., "minecraft", "create")
     */
    public static Set<String> getAllNamespaces() {
        Set<String> namespaces = new HashSet<>();
        for (StructureDefinition structure : structureDefinitions.values()) {
            String namespace = structure.getNamespace();
            if (!namespace.isEmpty()) {
                namespaces.add(namespace);
            }
        }
        return namespaces;
    }
    
    /**
     * Reload all structure definitions from JSON.
     * This clears the registry and reloads from the configuration file.
     */
    public static void reload() {
        AromaAffect.LOGGER.info("Reloading StructureRegistry...");
        structureDefinitions.clear();
        
        List<StructureDefinition> definitions = StructureDefinitionLoader.reload();
        for (StructureDefinition definition : definitions) {
            registerStructure(definition);
        }
        
        AromaAffect.LOGGER.info("StructureRegistry reloaded with {} structures", structureDefinitions.size());
    }
    
    /**
     * Clear the registry (primarily for testing).
     */
    static void clear() {
        structureDefinitions.clear();
        initialized = false;
    }
}

