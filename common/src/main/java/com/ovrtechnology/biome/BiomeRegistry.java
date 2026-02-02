package com.ovrtechnology.biome;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import lombok.Getter;

import java.util.*;

/**
 * Central registry for all trackable biome definitions in Aroma Affect.
 * 
 * <p>This class provides the main API for other systems to access biome information.
 * Biomes define which Minecraft biomes can be tracked by the Nose system, along with
 * their associated scents and display colors.</p>
 * 
 * <h2>Initialization Order</h2>
 * <p>BiomeRegistry must be initialized <strong>after</strong> ScentRegistry,
 * as it validates scent references during loading.</p>
 * 
 * <h2>Usage Example</h2>
 * <pre>
 * // Get a biome by ID
 * Optional&lt;BiomeDefinition&gt; biome = BiomeRegistry.getBiome("minecraft:jungle");
 * 
 * // Check if a biome is trackable
 * boolean canTrack = BiomeRegistry.hasBiome("minecraft:desert");
 * 
 * // Get the scent for a biome
 * Optional&lt;ScentDefinition&gt; scent = BiomeRegistry.getScentForBiome("minecraft:snowy_plains");
 * 
 * // Get all vanilla biomes
 * List&lt;BiomeDefinition&gt; vanilla = BiomeRegistry.getVanillaBiomes();
 * </pre>
 * 
 * <h2>Modded Biome Support</h2>
 * <p>The registry supports biomes from any mod. Biome IDs use the standard
 * Minecraft ResourceLocation format (namespace:path), allowing modded biomes
 * to be registered alongside vanilla ones.</p>
 */
public final class BiomeRegistry {
    
    /**
     * Map of biome ID to its definition
     */
    @Getter
    private static final Map<String, BiomeDefinition> biomeDefinitions = new LinkedHashMap<>();
    
    /**
     * Whether the registry has been initialized
     */
    @Getter
    private static boolean initialized = false;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private BiomeRegistry() {
        throw new UnsupportedOperationException("BiomeRegistry is a static utility class");
    }
    
    /**
     * Initialize the biome registry.
     * This loads biome definitions from JSON.
     * Must be called during mod initialization, after ScentRegistry.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("BiomeRegistry.init() called multiple times!");
            return;
        }
        
        // Warn if ScentRegistry is not initialized
        if (!ScentRegistry.isInitialized()) {
            AromaAffect.LOGGER.warn("BiomeRegistry.init() called before ScentRegistry! Scent validation may fail.");
        }
        
        AromaAffect.LOGGER.info("Initializing BiomeRegistry...");
        
        // Load biome definitions from JSON
        List<BiomeDefinition> definitions = BiomeDefinitionLoader.loadAllBiomes();
        
        // Store each biome in the map
        for (BiomeDefinition definition : definitions) {
            registerBiome(definition);
        }
        
        initialized = true;
        AromaAffect.LOGGER.info("BiomeRegistry initialized with {} biomes", biomeDefinitions.size());
    }
    
    /**
     * Register a single biome from its definition.
     * 
     * @param definition The biome definition to register
     */
    private static void registerBiome(BiomeDefinition definition) {
        String biomeId = definition.getBiomeId();
        
        if (biomeDefinitions.containsKey(biomeId)) {
            AromaAffect.LOGGER.warn("Duplicate biome ID in registry: {}, skipping...", biomeId);
            return;
        }
        
        biomeDefinitions.put(biomeId, definition);
        AromaAffect.LOGGER.debug("Registered biome: {}", biomeId);
    }
    
    /**
     * Get a biome definition by biome ID.
     * 
     * @param biomeId The Minecraft biome ID (e.g., "minecraft:jungle")
     * @return Optional containing the biome if found
     */
    public static Optional<BiomeDefinition> getBiome(String biomeId) {
        return Optional.ofNullable(biomeDefinitions.get(biomeId));
    }
    
    /**
     * Get a biome definition by ID, or throw if not found.
     * 
     * @param biomeId The biome ID
     * @return The biome definition
     * @throws IllegalArgumentException if biome not found
     */
    public static BiomeDefinition getBiomeOrThrow(String biomeId) {
        BiomeDefinition biome = biomeDefinitions.get(biomeId);
        if (biome == null) {
            throw new IllegalArgumentException("Unknown biome ID: " + biomeId);
        }
        return biome;
    }
    
    /**
     * Check if a biome with the given ID is registered.
     * 
     * @param biomeId The biome ID to check
     * @return true if the biome is trackable
     */
    public static boolean hasBiome(String biomeId) {
        return biomeDefinitions.containsKey(biomeId);
    }
    
    /**
     * Get all registered biome IDs.
     * 
     * @return Iterable of all biome IDs
     */
    public static Iterable<String> getAllBiomeIds() {
        return Collections.unmodifiableSet(biomeDefinitions.keySet());
    }
    
    /**
     * Get all registered biome definitions.
     * 
     * @return Iterable of all biome definitions
     */
    public static Iterable<BiomeDefinition> getAllBiomes() {
        return Collections.unmodifiableCollection(biomeDefinitions.values());
    }
    
    /**
     * Get all biome definitions as a list.
     * 
     * @return List of all biome definitions
     */
    public static List<BiomeDefinition> getAllBiomesAsList() {
        return new ArrayList<>(biomeDefinitions.values());
    }
    
    /**
     * Get the number of registered biomes.
     * 
     * @return The biome count
     */
    public static int getBiomeCount() {
        return biomeDefinitions.size();
    }
    
    /**
     * Get the scent definition associated with a biome.
     * 
     * @param biomeId The biome ID
     * @return Optional containing the scent if biome exists and has a scent
     */
    public static Optional<ScentDefinition> getScentForBiome(String biomeId) {
        BiomeDefinition biome = biomeDefinitions.get(biomeId);
        if (biome != null && biome.hasScentId()) {
            return ScentRegistry.getScent(biome.getScentId());
        }
        return Optional.empty();
    }
    
    /**
     * Get all biomes that use a specific scent.
     * 
     * @param scentId The scent ID to filter by
     * @return List of biomes using the specified scent
     */
    public static List<BiomeDefinition> getBiomesByScent(String scentId) {
        List<BiomeDefinition> result = new ArrayList<>();
        for (BiomeDefinition biome : biomeDefinitions.values()) {
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
        for (BiomeDefinition biome : biomeDefinitions.values()) {
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
        for (BiomeDefinition biome : biomeDefinitions.values()) {
            if (!biome.isVanilla()) {
                result.add(biome);
            }
        }
        return result;
    }
    
    /**
     * Get biomes by namespace (e.g., "minecraft", "terralith", "biomes_o_plenty").
     * 
     * @param namespace The mod namespace to filter by
     * @return List of biomes from that namespace
     */
    public static List<BiomeDefinition> getBiomesByNamespace(String namespace) {
        List<BiomeDefinition> result = new ArrayList<>();
        for (BiomeDefinition biome : biomeDefinitions.values()) {
            if (namespace.equals(biome.getNamespace())) {
                result.add(biome);
            }
        }
        return result;
    }
    
    /**
     * Get the display color for a biome as an integer.
     * 
     * @param biomeId The biome ID
     * @return RGB color as integer, or 0x5AA000 (green) if not found
     */
    public static int getBiomeColor(String biomeId) {
        BiomeDefinition biome = biomeDefinitions.get(biomeId);
        return biome != null ? biome.getColorAsInt() : 0x5AA000;
    }
    
    /**
     * Get the display color for a biome as HTML hex string.
     * 
     * @param biomeId The biome ID
     * @return HTML color string, or "#5AA000" if not found
     */
    public static String getBiomeColorHtml(String biomeId) {
        BiomeDefinition biome = biomeDefinitions.get(biomeId);
        return biome != null ? biome.getColorHtml() : BiomeDefinition.DEFAULT_COLOR;
    }
    
    /**
     * Validate that all biome IDs in a list are registered.
     * Useful for validating nose definitions that reference biomes.
     * 
     * @param biomeIds List of biome IDs to validate
     * @return List of invalid (unregistered) biome IDs
     */
    public static List<String> validateBiomeIds(List<String> biomeIds) {
        List<String> invalid = new ArrayList<>();
        for (String id : biomeIds) {
            if (!hasBiome(id)) {
                invalid.add(id);
            }
        }
        return invalid;
    }
    
    /**
     * Get all unique namespaces from registered biomes.
     * 
     * @return Set of namespace strings (e.g., "minecraft", "terralith")
     */
    public static Set<String> getAllNamespaces() {
        Set<String> namespaces = new HashSet<>();
        for (BiomeDefinition biome : biomeDefinitions.values()) {
            String namespace = biome.getNamespace();
            if (!namespace.isEmpty()) {
                namespaces.add(namespace);
            }
        }
        return namespaces;
    }
    
    /**
     * Reload all biome definitions from JSON.
     * This clears the registry and reloads from the configuration file.
     */
    public static void reload() {
        AromaAffect.LOGGER.info("Reloading BiomeRegistry...");
        biomeDefinitions.clear();
        
        List<BiomeDefinition> definitions = BiomeDefinitionLoader.reload();
        for (BiomeDefinition definition : definitions) {
            registerBiome(definition);
        }
        
        AromaAffect.LOGGER.info("BiomeRegistry reloaded with {} biomes", biomeDefinitions.size());
    }
    
    /**
     * Clear the registry (primarily for testing).
     */
    static void clear() {
        biomeDefinitions.clear();
        initialized = false;
    }
}

