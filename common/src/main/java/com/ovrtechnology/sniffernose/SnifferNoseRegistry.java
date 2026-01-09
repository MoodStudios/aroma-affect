package com.ovrtechnology.sniffernose;

import com.ovrtechnology.AromaCraft;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.*;

/**
 * Central registry for all sniffer nose items in AromaCraft.
 * 
 * <p>This class handles:</p>
 * <ul>
 *   <li>Loading sniffer nose definitions from JSON</li>
 *   <li>Registering sniffer nose items with Minecraft's registry system</li>
 *   <li>Providing access to registered sniffer nose items</li>
 * </ul>
 * 
 * <p>Sniffer noses are separate from regular player-equippable noses.
 * They are designed for the Sniffer mob.</p>
 */
public final class SnifferNoseRegistry {
    
    /**
     * Deferred register for sniffer nose items
     */
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(AromaCraft.MOD_ID, Registries.ITEM);
    
    /**
     * Map of sniffer nose ID to registered item supplier
     */
    @Getter
    private static final Map<String, RegistrySupplier<SnifferNoseItem>> snifferNoseItems = new LinkedHashMap<>();
    
    /**
     * Map of sniffer nose ID to its definition
     */
    @Getter
    private static final Map<String, SnifferNoseDefinition> snifferNoseDefinitions = new LinkedHashMap<>();
    
    /**
     * Whether the registry has been initialized
     */
    @Getter
    private static boolean initialized = false;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private SnifferNoseRegistry() {
        throw new UnsupportedOperationException("SnifferNoseRegistry is a static utility class");
    }
    
    /**
     * Initialize the sniffer nose registry.
     * This loads sniffer nose definitions from JSON and registers items.
     * Must be called during mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaCraft.LOGGER.warn("SnifferNoseRegistry.init() called multiple times!");
            return;
        }
        
        AromaCraft.LOGGER.info("Initializing SnifferNoseRegistry...");
        
        // Load sniffer nose definitions from JSON
        List<SnifferNoseDefinition> definitions = SnifferNoseDefinitionLoader.loadAllSnifferNoses();
        
        // Register each sniffer nose as an item
        for (SnifferNoseDefinition definition : definitions) {
            registerSnifferNose(definition);
        }
        
        // Register the deferred register with Architectury
        ITEMS.register();
        
        initialized = true;
        AromaCraft.LOGGER.info("SnifferNoseRegistry initialized with {} sniffer noses", snifferNoseItems.size());
    }
    
    /**
     * Register a single sniffer nose from its definition
     */
    private static void registerSnifferNose(SnifferNoseDefinition definition) {
        String id = definition.getId();
        
        if (snifferNoseItems.containsKey(id)) {
            AromaCraft.LOGGER.warn("Duplicate sniffer nose ID: {}, skipping...", id);
            return;
        }
        
        // Store the definition
        snifferNoseDefinitions.put(id, definition);
        
        // Register the item
        final String itemId = id;
        RegistrySupplier<SnifferNoseItem> supplier = ITEMS.register(id, () -> new SnifferNoseItem(definition, itemId));
        snifferNoseItems.put(id, supplier);
        
        AromaCraft.LOGGER.debug("Registered sniffer nose item: {}", id);
    }
    
    /**
     * Get a registered sniffer nose item by ID
     */
    public static Optional<SnifferNoseItem> getSnifferNose(String id) {
        RegistrySupplier<SnifferNoseItem> supplier = snifferNoseItems.get(id);
        if (supplier != null && supplier.isPresent()) {
            return Optional.of(supplier.get());
        }
        return Optional.empty();
    }
    
    /**
     * Get the supplier for a sniffer nose item
     */
    public static Optional<RegistrySupplier<SnifferNoseItem>> getSnifferNoseSupplier(String id) {
        return Optional.ofNullable(snifferNoseItems.get(id));
    }
    
    /**
     * Get a sniffer nose definition by ID
     */
    public static Optional<SnifferNoseDefinition> getDefinition(String id) {
        return Optional.ofNullable(snifferNoseDefinitions.get(id));
    }
    
    /**
     * Get all registered sniffer nose IDs
     */
    public static Iterable<String> getAllSnifferNoseIds() {
        return Collections.unmodifiableSet(snifferNoseItems.keySet());
    }
    
    /**
     * Get all registered sniffer nose items
     */
    public static Iterable<RegistrySupplier<SnifferNoseItem>> getAllSnifferNoses() {
        return Collections.unmodifiableCollection(snifferNoseItems.values());
    }
    
    /**
     * Get all registered sniffer nose items as a list
     */
    public static List<SnifferNoseItem> getAllSnifferNosesAsList() {
        List<SnifferNoseItem> result = new ArrayList<>();
        for (RegistrySupplier<SnifferNoseItem> supplier : snifferNoseItems.values()) {
            if (supplier.isPresent()) {
                result.add(supplier.get());
            }
        }
        return result;
    }
    
    /**
     * Get the number of registered sniffer noses
     */
    public static int getSnifferNoseCount() {
        return snifferNoseItems.size();
    }
    
    /**
     * Check if a sniffer nose with the given ID is registered
     */
    public static boolean hasSnifferNose(String id) {
        return snifferNoseItems.containsKey(id);
    }
    
    /**
     * Get the deferred register (for internal use)
     */
    static DeferredRegister<Item> getItemRegister() {
        return ITEMS;
    }
}
