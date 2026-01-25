package com.ovrtechnology.nose;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.*;

/**
 * Central registry for all nose items in Aroma Affect.
 * 
 * This class handles:
 * - Loading nose definitions from JSON
 * - Registering nose items with Minecraft's registry system
 * - Providing access to registered nose items
 * - Initializing the ability resolver for inheritance
 * 
 * Note: Recipes are loaded from data/aromaaffect/recipe/ as standard Minecraft recipe JSON files.
 */
public final class NoseRegistry {
    
    /**
     * Deferred register for nose items
     */
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(AromaAffect.MOD_ID, Registries.ITEM);
    
    /**
     * Map of nose ID to registered item supplier
     */
    @Getter
    private static final Map<String, RegistrySupplier<NoseItem>> noseItems = new HashMap<>();
    
    /**
     * Map of nose ID to its definition
     */
    @Getter
    private static final Map<String, NoseDefinition> noseDefinitions = new HashMap<>();
    
    /**
     * Whether the registry has been initialized
     */
    @Getter
    private static boolean initialized = false;
    
    /**
     * Initialize the nose registry.
     * This loads nose definitions from JSON and registers items.
     * Must be called during mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("NoseRegistry.init() called multiple times!");
            return;
        }
        
        AromaAffect.LOGGER.info("Initializing NoseRegistry...");
        
        // Load nose definitions from JSON
        List<NoseDefinition> definitions = NoseDefinitionLoader.loadAllNoses();
        
        // Register each nose as an item
        for (NoseDefinition definition : definitions) {
            registerNose(definition);
        }
        
        // Register the deferred register with Architectury
        ITEMS.register();
        
        // Initialize the ability resolver (handles inheritance and caching)
        NoseAbilityResolver.init();
        
        initialized = true;
        AromaAffect.LOGGER.info("NoseRegistry initialized with {} noses", noseItems.size());
    }
    
    /**
     * Register a single nose from its definition
     */
    private static void registerNose(NoseDefinition definition) {
        String id = definition.getId();
        
        if (noseItems.containsKey(id)) {
            AromaAffect.LOGGER.warn("Duplicate nose ID: {}, skipping...", id);
            return;
        }
        
        // Store the definition
        noseDefinitions.put(id, definition);
        
        // Register the item - pass the ID for ResourceKey creation
        final String itemId = id; // Capture for lambda
        RegistrySupplier<NoseItem> supplier = ITEMS.register(id, () -> new NoseItem(definition, itemId));
        noseItems.put(id, supplier);
        
        AromaAffect.LOGGER.debug("Registered nose item: {}", id);
    }
    
    /**
     * Get a registered nose item by ID
     */
    public static Optional<NoseItem> getNose(String id) {
        RegistrySupplier<NoseItem> supplier = noseItems.get(id);
        if (supplier != null && supplier.isPresent()) {
            return Optional.of(supplier.get());
        }
        return Optional.empty();
    }
    
    /**
     * Get the supplier for a nose item
     */
    public static Optional<RegistrySupplier<NoseItem>> getNoseSupplier(String id) {
        return Optional.ofNullable(noseItems.get(id));
    }
    
    /**
     * Get a nose definition by ID
     */
    public static Optional<NoseDefinition> getDefinition(String id) {
        return Optional.ofNullable(noseDefinitions.get(id));
    }
    
    /**
     * Get all registered nose IDs
     */
    public static Iterable<String> getAllNoseIds() {
        return noseItems.keySet();
    }
    
    /**
     * Get all registered nose items
     */
    public static Iterable<RegistrySupplier<NoseItem>> getAllNoses() {
        return noseItems.values();
    }

    /**
     * Get all registered nose items as a list.
     * @return A list of all registered nose items.
     */
    public static List<NoseItem> getAllNosesAsList() {
        return new ArrayList<>(noseItems.values()).stream().map(RegistrySupplier::get).toList();
    }

    /**
     * Get the number of registered noses
     */
    public static int getNoseCount() {
        return noseItems.size();
    }
    
    /**
     * Check if a nose with the given ID is registered
     */
    public static boolean hasNose(String id) {
        return noseItems.containsKey(id);
    }
    
    /**
     * Get the deferred register (for internal use)
     */
    static DeferredRegister<Item> getItemRegister() {
        return ITEMS;
    }
}
