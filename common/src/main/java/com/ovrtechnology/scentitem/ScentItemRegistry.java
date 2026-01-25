package com.ovrtechnology.scentitem;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.*;

/**
 * Central registry for all scent items in Aroma Affect.
 * 
 * <p>This class handles:</p>
 * <ul>
 *   <li>Loading scent item definitions from JSON</li>
 *   <li>Registering scent items with Minecraft's registry system</li>
 *   <li>Providing access to registered scent items</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>
 * // Initialize during mod startup
 * ScentItemRegistry.init();
 * 
 * // Get a scent item by ID
 * Optional&lt;ScentItem&gt; item = ScentItemRegistry.getScentItem("winter_scent");
 * 
 * // Get all scent items
 * List&lt;ScentItem&gt; allItems = ScentItemRegistry.getAllScentItemsAsList();
 * </pre>
 */
public final class ScentItemRegistry {
    
    /**
     * Deferred register for scent items
     */
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(AromaAffect.MOD_ID, Registries.ITEM);
    
    /**
     * Map of scent item ID to registered item supplier
     */
    @Getter
    private static final Map<String, RegistrySupplier<ScentItem>> scentItems = new LinkedHashMap<>();
    
    /**
     * Map of scent item ID to its definition
     */
    @Getter
    private static final Map<String, ScentItemDefinition> scentItemDefinitions = new LinkedHashMap<>();
    
    /**
     * Whether the registry has been initialized
     */
    @Getter
    private static boolean initialized = false;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ScentItemRegistry() {
        throw new UnsupportedOperationException("ScentItemRegistry is a static utility class");
    }
    
    /**
     * Initialize the scent item registry.
     * This loads scent item definitions from JSON and registers items.
     * Must be called during mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("ScentItemRegistry.init() called multiple times!");
            return;
        }
        
        AromaAffect.LOGGER.info("Initializing ScentItemRegistry...");
        
        // Load scent item definitions from JSON
        List<ScentItemDefinition> definitions = ScentItemDefinitionLoader.loadAllScentItems();
        
        // Register each scent item
        for (ScentItemDefinition definition : definitions) {
            registerScentItem(definition);
        }
        
        // Register the deferred register with Architectury
        ITEMS.register();
        
        initialized = true;
        AromaAffect.LOGGER.info("ScentItemRegistry initialized with {} scent items", scentItems.size());
    }
    
    /**
     * Register a single scent item from its definition.
     */
    private static void registerScentItem(ScentItemDefinition definition) {
        String id = definition.getId();
        
        if (scentItems.containsKey(id)) {
            AromaAffect.LOGGER.warn("Duplicate scent item ID: {}, skipping...", id);
            return;
        }
        
        // Store the definition
        scentItemDefinitions.put(id, definition);
        
        // Register the item
        final String itemId = id;
        RegistrySupplier<ScentItem> supplier = ITEMS.register(id, () -> new ScentItem(definition, itemId));
        scentItems.put(id, supplier);
        
        AromaAffect.LOGGER.debug("Registered scent item: {}", id);
    }
    
    /**
     * Get a registered scent item by ID.
     * 
     * @param id The scent item ID
     * @return Optional containing the scent item if found
     */
    public static Optional<ScentItem> getScentItem(String id) {
        RegistrySupplier<ScentItem> supplier = scentItems.get(id);
        if (supplier != null && supplier.isPresent()) {
            return Optional.of(supplier.get());
        }
        return Optional.empty();
    }
    
    /**
     * Get the supplier for a scent item.
     * 
     * @param id The scent item ID
     * @return Optional containing the supplier if found
     */
    public static Optional<RegistrySupplier<ScentItem>> getScentItemSupplier(String id) {
        return Optional.ofNullable(scentItems.get(id));
    }
    
    /**
     * Get a scent item definition by ID.
     * 
     * @param id The scent item ID
     * @return Optional containing the definition if found
     */
    public static Optional<ScentItemDefinition> getDefinition(String id) {
        return Optional.ofNullable(scentItemDefinitions.get(id));
    }
    
    /**
     * Get all registered scent item IDs.
     * 
     * @return Iterable of all scent item IDs
     */
    public static Iterable<String> getAllScentItemIds() {
        return Collections.unmodifiableSet(scentItems.keySet());
    }
    
    /**
     * Get all registered scent items.
     * 
     * @return Iterable of all scent item suppliers
     */
    public static Iterable<RegistrySupplier<ScentItem>> getAllScentItems() {
        return Collections.unmodifiableCollection(scentItems.values());
    }
    
    /**
     * Get all registered scent items as a list.
     * 
     * @return A list of all registered scent items
     */
    public static List<ScentItem> getAllScentItemsAsList() {
        List<ScentItem> result = new ArrayList<>();
        for (RegistrySupplier<ScentItem> supplier : scentItems.values()) {
            if (supplier.isPresent()) {
                result.add(supplier.get());
            }
        }
        return result;
    }
    
    /**
     * Get the number of registered scent items.
     * 
     * @return The scent item count
     */
    public static int getScentItemCount() {
        return scentItems.size();
    }
    
    /**
     * Check if a scent item with the given ID is registered.
     * 
     * @param id The scent item ID to check
     * @return true if the scent item exists
     */
    public static boolean hasScentItem(String id) {
        return scentItems.containsKey(id);
    }
    
    /**
     * Get scent items sorted by priority (highest first).
     * 
     * @return List of scent items sorted by priority descending
     */
    public static List<ScentItem> getScentItemsSortedByPriority() {
        List<ScentItem> result = getAllScentItemsAsList();
        result.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return result;
    }
    
    /**
     * Get the deferred register (for internal use).
     */
    static DeferredRegister<Item> getItemRegister() {
        return ITEMS;
    }
}
