package com.ovrtechnology.trigger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ovrtechnology.AromaCraft;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads and provides access to scent trigger configurations.
 * 
 * <p>This class handles loading the scent_triggers.json file and provides
 * fast lookup methods for finding triggers by item ID, biome ID, etc.</p>
 */
public final class ScentTriggerConfigLoader {
    
    /**
     * Path to the trigger configuration file.
     */
    private static final String CONFIG_PATH = "data/aromacraft/scents/scent_triggers.json";
    
    /**
     * Valid OVR scent names (case-sensitive).
     * Only these names are supported by the OVR hardware bridge.
     */
    public static final Set<String> VALID_OVR_SCENTS = Set.of(
        "Beach", "Evergreen", "Desert", "Floral", "Barnyard",
        "Smoky", "Winter", "Terra Silva", "Savory Spice", "Timber",
        "Petrichor", "Sweet", "Machina", "Marine", "Kindred", "Citrus"
    );
    
    /**
     * GSON instance for parsing.
     */
    private static final Gson GSON = new GsonBuilder()
            .setLenient()
            .create();
    
    /**
     * The loaded configuration.
     */
    private static TriggerConfigRoot config;
    
    /**
     * Lookup map: item_id -> ItemTriggerDefinition
     */
    private static final Map<String, ItemTriggerDefinition> itemTriggerMap = new HashMap<>();
    
    /**
     * Lookup map: biome_id -> BiomeTriggerDefinition
     */
    private static final Map<String, BiomeTriggerDefinition> biomeTriggerMap = new HashMap<>();
    
    /**
     * Lookup map: block_id -> BlockTriggerDefinition
     */
    private static final Map<String, BlockTriggerDefinition> blockTriggerMap = new HashMap<>();
    
    /**
     * Lookup map: entity_type -> MobTriggerDefinition
     */
    private static final Map<String, MobTriggerDefinition> mobTriggerMap = new HashMap<>();
    
    /**
     * Whether the loader has been initialized.
     */
    private static boolean initialized = false;
    
    private ScentTriggerConfigLoader() {
    }
    
    /**
     * Initializes the trigger configuration loader.
     * Loads and validates the scent_triggers.json file.
     */
    public static void init() {
        if (initialized) {
            AromaCraft.LOGGER.warn("ScentTriggerConfigLoader.init() called multiple times!");
            return;
        }
        
        AromaCraft.LOGGER.info("Loading scent trigger configuration...");
        
        try {
            config = loadConfigFromResource();
            if (config != null) {
                config.validate();
                buildLookupMaps();
                AromaCraft.LOGGER.info("Loaded {} scent triggers ({} item, {} biome, {} block, {} mob)",
                        config.getTotalTriggerCount(),
                        config.getItemTriggers().size(),
                        config.getBiomeTriggers().size(),
                        config.getBlockTriggers().size(),
                        config.getMobTriggers().size());
            } else {
                AromaCraft.LOGGER.warn("Failed to load scent trigger configuration, using defaults");
                config = new TriggerConfigRoot();
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Error loading scent trigger configuration", e);
            config = new TriggerConfigRoot();
        }
        
        initialized = true;
    }
    
    /**
     * Loads the configuration from the resource file.
     * 
     * @return the parsed configuration, or null if not found
     */
    private static TriggerConfigRoot loadConfigFromResource() {
        try (InputStream is = ScentTriggerConfigLoader.class.getClassLoader()
                .getResourceAsStream(CONFIG_PATH)) {
            
            if (is == null) {
                AromaCraft.LOGGER.warn("Scent trigger config not found: {}", CONFIG_PATH);
                return null;
            }
            
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, TriggerConfigRoot.class);
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Error reading scent trigger config: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Builds the lookup maps from the loaded configuration.
     */
    private static void buildLookupMaps() {
        itemTriggerMap.clear();
        biomeTriggerMap.clear();
        blockTriggerMap.clear();
        mobTriggerMap.clear();
        
        // Build item trigger map
        for (ItemTriggerDefinition trigger : config.getItemTriggers()) {
            if (trigger.isValid()) {
                validateScentName(trigger.getScentName(), "item", trigger.getItemId());
                itemTriggerMap.put(trigger.getItemId(), trigger);
                AromaCraft.LOGGER.debug("Registered item trigger: {} -> {}",
                        trigger.getItemId(), trigger.getScentName());
            } else {
                AromaCraft.LOGGER.warn("Invalid item trigger definition: {}", trigger);
            }
        }
        
        // Build biome trigger map (placeholder, but log for awareness)
        for (BiomeTriggerDefinition trigger : config.getBiomeTriggers()) {
            if (trigger.isValid()) {
                validateScentName(trigger.getScentName(), "biome", trigger.getBiomeId());
                biomeTriggerMap.put(trigger.getBiomeId(), trigger);
                AromaCraft.LOGGER.debug("Registered biome trigger: {} -> {} (placeholder)",
                        trigger.getBiomeId(), trigger.getScentName());
            }
        }
        
        // Build block trigger map (placeholder)
        for (BlockTriggerDefinition trigger : config.getBlockTriggers()) {
            if (trigger.isValid()) {
                validateScentName(trigger.getScentName(), "block", trigger.getBlockId());
                blockTriggerMap.put(trigger.getBlockId(), trigger);
                AromaCraft.LOGGER.debug("Registered block trigger: {} -> {} (placeholder)",
                        trigger.getBlockId(), trigger.getScentName());
            }
        }
        
        // Build mob trigger map (placeholder)
        for (MobTriggerDefinition trigger : config.getMobTriggers()) {
            if (trigger.isValid()) {
                validateScentName(trigger.getScentName(), "mob", trigger.getEntityType());
                mobTriggerMap.put(trigger.getEntityType(), trigger);
                AromaCraft.LOGGER.debug("Registered mob trigger: {} -> {} (placeholder)",
                        trigger.getEntityType(), trigger.getScentName());
            }
        }
    }
    
    /**
     * Validates that a scent name is in the list of valid OVR scents.
     * Logs a warning if not valid.
     */
    private static void validateScentName(String scentName, String triggerType, String triggerId) {
        if (!VALID_OVR_SCENTS.contains(scentName)) {
            AromaCraft.LOGGER.warn("Unknown OVR scent '{}' in {} trigger '{}'. " +
                    "This scent may not be supported by the hardware.",
                    scentName, triggerType, triggerId);
        }
    }
    
    // ========================================
    // Lookup Methods
    // ========================================
    
    /**
     * Gets the item trigger definition for a given item ID.
     * 
     * @param itemId the full item ID (e.g., "aromacraft:winter_scent")
     * @return Optional containing the trigger if found
     */
    public static Optional<ItemTriggerDefinition> getItemTrigger(String itemId) {
        return Optional.ofNullable(itemTriggerMap.get(itemId));
    }
    
    /**
     * Gets the biome trigger definition for a given biome ID.
     * 
     * @param biomeId the biome ID (e.g., "minecraft:forest")
     * @return Optional containing the trigger if found
     */
    public static Optional<BiomeTriggerDefinition> getBiomeTrigger(String biomeId) {
        return Optional.ofNullable(biomeTriggerMap.get(biomeId));
    }
    
    /**
     * Gets the block trigger definition for a given block ID.
     * 
     * @param blockId the block ID (e.g., "minecraft:campfire")
     * @return Optional containing the trigger if found
     */
    public static Optional<BlockTriggerDefinition> getBlockTrigger(String blockId) {
        return Optional.ofNullable(blockTriggerMap.get(blockId));
    }
    
    /**
     * Gets the mob trigger definition for a given entity type.
     * 
     * @param entityType the entity type ID (e.g., "minecraft:cow")
     * @return Optional containing the trigger if found
     */
    public static Optional<MobTriggerDefinition> getMobTrigger(String entityType) {
        return Optional.ofNullable(mobTriggerMap.get(entityType));
    }
    
    /**
     * Checks if an item has a trigger configured.
     * 
     * @param itemId the item ID to check
     * @return true if a trigger exists
     */
    public static boolean hasItemTrigger(String itemId) {
        return itemTriggerMap.containsKey(itemId);
    }
    
    /**
     * Gets all item triggers.
     * 
     * @return unmodifiable collection of item triggers
     */
    public static Collection<ItemTriggerDefinition> getAllItemTriggers() {
        return Collections.unmodifiableCollection(itemTriggerMap.values());
    }
    
    /**
     * Gets the global trigger settings.
     * 
     * @return the settings, or defaults if not loaded
     */
    public static TriggerSettings getSettings() {
        return config != null ? config.getSettings() : TriggerSettings.defaults();
    }
    
    /**
     * Checks if the loader has been initialized.
     * 
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Reloads the configuration from file.
     * Useful for hot-reloading during development.
     */
    public static void reload() {
        AromaCraft.LOGGER.info("Reloading scent trigger configuration...");
        initialized = false;
        init();
    }
}
