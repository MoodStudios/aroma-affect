package com.ovrtechnology.scent;

import com.ovrtechnology.AromaAffect;
import lombok.Getter;

import java.util.*;

/**
 * Central registry for all scent definitions in Aroma Affect.
 * 
 * <p>This class provides the main API for other systems to access scent information.
 * It handles:</p>
 * <ul>
 *   <li>Loading scent definitions from JSON</li>
 *   <li>Providing access to scent definitions by ID</li>
 *   <li>Filtering scents by priority</li>
 *   <li>Validation of scent references</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>
 * // Initialize during mod startup
 * ScentRegistry.init();
 * 
 * // Get a scent by ID
 * Optional&lt;ScentDefinition&gt; scent = ScentRegistry.getScent("winter");
 * 
 * // Check if a scent exists
 * boolean exists = ScentRegistry.hasScent("beach");
 * 
 * // Get all scent IDs
 * Iterable&lt;String&gt; allIds = ScentRegistry.getAllScentIds();
 * </pre>
 */
public final class ScentRegistry {
    
    /**
     * Map of scent ID to its definition
     */
    @Getter
    private static final Map<String, ScentDefinition> scentDefinitions = new LinkedHashMap<>();
    
    /**
     * Whether the registry has been initialized
     */
    @Getter
    private static boolean initialized = false;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ScentRegistry() {
        throw new UnsupportedOperationException("ScentRegistry is a static utility class");
    }
    
    /**
     * Initialize the scent registry.
     * This loads scent definitions from JSON.
     * Must be called during mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("ScentRegistry.init() called multiple times!");
            return;
        }
        
        AromaAffect.LOGGER.info("Initializing ScentRegistry...");
        
        // Load scent definitions from JSON
        List<ScentDefinition> definitions = ScentDefinitionLoader.loadAllScents();
        
        // Store each scent in the map
        for (ScentDefinition definition : definitions) {
            registerScent(definition);
        }
        
        initialized = true;
        AromaAffect.LOGGER.info("ScentRegistry initialized with {} scents", scentDefinitions.size());
    }
    
    /**
     * Register a single scent from its definition.
     * 
     * @param definition The scent definition to register
     */
    private static void registerScent(ScentDefinition definition) {
        String id = definition.getId();
        
        if (scentDefinitions.containsKey(id)) {
            AromaAffect.LOGGER.warn("Duplicate scent ID in registry: {}, skipping...", id);
            return;
        }
        
        scentDefinitions.put(id, definition);
        AromaAffect.LOGGER.debug("Registered scent: {}", id);
    }
    
    /**
     * Get a scent definition by ID.
     * 
     * @param id The scent ID
     * @return Optional containing the scent if found
     */
    public static Optional<ScentDefinition> getScent(String id) {
        return Optional.ofNullable(scentDefinitions.get(id));
    }
    
    /**
     * Get a scent definition by ID, or throw if not found.
     * 
     * @param id The scent ID
     * @return The scent definition
     * @throws IllegalArgumentException if scent not found
     */
    public static ScentDefinition getScentOrThrow(String id) {
        ScentDefinition scent = scentDefinitions.get(id);
        if (scent == null) {
            throw new IllegalArgumentException("Unknown scent ID: " + id);
        }
        return scent;
    }
    
    /**
     * Get all registered scent IDs.
     * 
     * @return Iterable of all scent IDs
     */
    public static Iterable<String> getAllScentIds() {
        return Collections.unmodifiableSet(scentDefinitions.keySet());
    }
    
    /**
     * Get all registered scent definitions.
     * 
     * @return Iterable of all scent definitions
     */
    public static Iterable<ScentDefinition> getAllScents() {
        return Collections.unmodifiableCollection(scentDefinitions.values());
    }
    
    /**
     * Get all scent definitions as a list.
     * 
     * @return List of all scent definitions
     */
    public static List<ScentDefinition> getAllScentsAsList() {
        return new ArrayList<>(scentDefinitions.values());
    }
    
    /**
     * Get the number of registered scents.
     * 
     * @return The scent count
     */
    public static int getScentCount() {
        return scentDefinitions.size();
    }
    
    /**
     * Check if a scent with the given ID is registered.
     * 
     * @param id The scent ID to check
     * @return true if the scent exists
     */
    public static boolean hasScent(String id) {
        return scentDefinitions.containsKey(id);
    }
    
    /**
     * Get scents sorted by ID alphabetically.
     * 
     * @return List of scents sorted by ID
     */
    public static List<ScentDefinition> getScentsSortedById() {
        List<ScentDefinition> result = new ArrayList<>(scentDefinitions.values());
        result.sort((a, b) -> a.getId().compareTo(b.getId()));
        return result;
    }
    
    /**
     * Validate that all scent IDs in a list are registered.
     * Useful for validating configuration that references scents.
     * 
     * @param scentIds List of scent IDs to validate
     * @return List of invalid (unregistered) scent IDs
     */
    public static List<String> validateScentIds(List<String> scentIds) {
        List<String> invalid = new ArrayList<>();
        for (String id : scentIds) {
            if (!hasScent(id)) {
                invalid.add(id);
            }
        }
        return invalid;
    }
    
    /**
     * Get the display name for a scent, using localization if available.
     * Falls back to the fallback_name from the definition.
     * 
     * @param id The scent ID
     * @return The display name, or "Unknown Scent" if not found
     */
    public static String getDisplayName(String id) {
        ScentDefinition scent = scentDefinitions.get(id);
        if (scent == null) {
            return "Unknown Scent";
        }
        // TODO: Integrate with Minecraft's localization system
        // For now, return the fallback name
        return scent.getFallbackName();
    }
    
    /**
     * Reload all scent definitions from JSON.
     * This clears the registry and reloads from the configuration file.
     */
    public static void reload() {
        AromaAffect.LOGGER.info("Reloading ScentRegistry...");
        scentDefinitions.clear();
        
        List<ScentDefinition> definitions = ScentDefinitionLoader.reload();
        for (ScentDefinition definition : definitions) {
            registerScent(definition);
        }
        
        AromaAffect.LOGGER.info("ScentRegistry reloaded with {} scents", scentDefinitions.size());
    }
    
    /**
     * Clear the registry (primarily for testing).
     */
    static void clear() {
        scentDefinitions.clear();
        initialized = false;
    }
}

