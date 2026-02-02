package com.ovrtechnology.trigger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.biome.BiomeDefinition;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
import com.ovrtechnology.block.BlockDefinition;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.flower.FlowerDefinition;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.mob.MobDefinition;
import com.ovrtechnology.mob.MobDefinitionLoader;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.structure.StructureDefinition;
import com.ovrtechnology.structure.StructureDefinitionLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads and provides access to scent trigger configurations.
 *
 * <p>This class builds trigger lookup maps from per-category definition loaders
 * (biomes, blocks, flowers, structures, mobs) and a separate item triggers file.</p>
 */
public final class ScentTriggerConfigLoader {

    /**
     * Path to the item triggers configuration file (items remain separate).
     */
    private static final String ITEM_TRIGGERS_PATH = "data/aromaaffect/scents/scent_item_triggers.json";

    /**
     * Path to the trigger settings file.
     */
    private static final String SETTINGS_PATH = "data/aromaaffect/scents/trigger_settings.json";

    /**
     * Valid OVR scent names (case-sensitive).
     * Only these names are supported by the OVR hardware bridge.
     */
    public static final Set<String> VALID_OVR_SCENTS = Set.of(
        "Beach", "Evergreen", "Desert", "Floral", "Barnyard",
        "Smoky", "Winter", "Terra Silva", "Savory Spice", "Timber",
        "Petrichor", "Sweet", "Machina", "Marine", "Kindred", "Citrus"
    );

    private static final Gson GSON = new GsonBuilder()
            .setLenient()
            .create();

    private static TriggerSettings settings;

    private static final Map<String, ItemTriggerDefinition> itemTriggerMap = new HashMap<>();
    private static final Map<String, BiomeTriggerDefinition> biomeTriggerMap = new HashMap<>();
    private static final Map<String, BlockTriggerDefinition> blockTriggerMap = new HashMap<>();
    private static final Map<String, MobTriggerDefinition> mobTriggerMap = new HashMap<>();
    private static final Map<String, StructureTriggerDefinition> structureTriggerMap = new HashMap<>();

    private static boolean initialized = false;

    private ScentTriggerConfigLoader() {
    }

    /**
     * Initializes the trigger configuration loader.
     * Builds trigger maps from per-category definition loaders.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("ScentTriggerConfigLoader.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Loading scent trigger configuration...");

        try {
            // Load settings
            settings = loadSettings();
            if (settings == null) {
                settings = TriggerSettings.defaults();
            }
            settings.validate();

            // Load item triggers from dedicated file
            loadItemTriggers();

            // Build trigger maps from per-category loaders
            buildBiomeTriggers();
            buildBlockTriggers();
            buildFlowerTriggers();
            buildStructureTriggers();
            buildMobTriggers();

            int total = itemTriggerMap.size() + biomeTriggerMap.size()
                    + blockTriggerMap.size() + mobTriggerMap.size()
                    + structureTriggerMap.size();

            AromaAffect.LOGGER.info("Loaded {} scent triggers ({} item, {} biome, {} block, {} mob, {} structure)",
                    total,
                    itemTriggerMap.size(),
                    biomeTriggerMap.size(),
                    blockTriggerMap.size(),
                    mobTriggerMap.size(),
                    structureTriggerMap.size());
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error loading scent trigger configuration", e);
            if (settings == null) {
                settings = TriggerSettings.defaults();
            }
        }

        initialized = true;
    }

    /**
     * Loads trigger settings from the settings JSON file.
     */
    private static TriggerSettings loadSettings() {
        try (InputStream is = ScentTriggerConfigLoader.class.getClassLoader()
                .getResourceAsStream(SETTINGS_PATH)) {
            if (is == null) {
                AromaAffect.LOGGER.warn("Trigger settings not found: {}, using defaults", SETTINGS_PATH);
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, TriggerSettings.class);
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error reading trigger settings: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Loads item triggers from the scent_items.json file.
     */
    private static void loadItemTriggers() {
        itemTriggerMap.clear();
        try (InputStream is = ScentTriggerConfigLoader.class.getClassLoader()
                .getResourceAsStream(ITEM_TRIGGERS_PATH)) {
            if (is == null) {
                AromaAffect.LOGGER.warn("Item triggers file not found: {}", ITEM_TRIGGERS_PATH);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                ItemTriggersRoot root = GSON.fromJson(reader, ItemTriggersRoot.class);
                if (root != null && root.getItemTriggers() != null) {
                    for (ItemTriggerDefinition trigger : root.getItemTriggers()) {
                        if (trigger.isValid()) {
                            validateScentName(trigger.getScentName(), "item", trigger.getItemId());
                            itemTriggerMap.put(trigger.getItemId(), trigger);
                        }
                    }
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error loading item triggers: {}", e.getMessage());
        }
    }

    /**
     * Resolves a scent_id (lowercase) to the OVR display name via ScentRegistry.
     */
    private static String resolveScentName(String scentId) {
        if (scentId == null || scentId.isEmpty()) {
            return null;
        }
        return ScentRegistry.getDisplayName(scentId);
    }

    /**
     * Builds biome trigger map from BiomeDefinitionLoader.
     */
    private static void buildBiomeTriggers() {
        biomeTriggerMap.clear();
        for (BiomeDefinition biome : BiomeDefinitionLoader.getLoadedBiomes()) {
            if (!biome.hasScentId()) continue;

            String scentName = resolveScentName(biome.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) {
                AromaAffect.LOGGER.warn("Could not resolve scent_id '{}' for biome '{}'",
                        biome.getScentId(), biome.getBiomeId());
                continue;
            }

            BiomeTriggerDefinition trigger = new BiomeTriggerDefinition();
            trigger.setBiomeId(biome.getBiomeId());
            trigger.setScentName(scentName);
            trigger.setMode(biome.getMode());
            trigger.setPriority(biome.getPriority());
            trigger.setIntensity(biome.getIntensity());

            validateScentName(scentName, "biome", biome.getBiomeId());
            biomeTriggerMap.put(biome.getBiomeId(), trigger);
            AromaAffect.LOGGER.debug("Registered biome trigger: {} -> {}",
                    biome.getBiomeId(), scentName);
        }
    }

    /**
     * Builds block trigger map from BlockDefinitionLoader.
     */
    private static void buildBlockTriggers() {
        blockTriggerMap.clear();
        for (BlockDefinition block : BlockDefinitionLoader.getLoadedBlocks()) {
            if (!block.hasScentId()) continue;

            String scentName = resolveScentName(block.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) {
                AromaAffect.LOGGER.warn("Could not resolve scent_id '{}' for block '{}'",
                        block.getScentId(), block.getBlockId());
                continue;
            }

            BlockTriggerDefinition trigger = new BlockTriggerDefinition();
            trigger.setBlockId(block.getBlockId());
            trigger.setScentName(scentName);
            trigger.setTriggerOn(block.getTriggerOn());
            trigger.setRange(block.getRange());
            trigger.setPriority(block.getPriority());
            trigger.setIntensity(block.getIntensity());

            validateScentName(scentName, "block", block.getBlockId());
            blockTriggerMap.put(block.getBlockId(), trigger);
            AromaAffect.LOGGER.debug("Registered block trigger: {} -> {}",
                    block.getBlockId(), scentName);
        }
    }

    /**
     * Builds flower triggers (as block triggers) from FlowerDefinitionLoader.
     */
    private static void buildFlowerTriggers() {
        for (FlowerDefinition flower : FlowerDefinitionLoader.getLoadedFlowers()) {
            if (!flower.hasScentId()) continue;

            String scentName = resolveScentName(flower.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) {
                AromaAffect.LOGGER.warn("Could not resolve scent_id '{}' for flower '{}'",
                        flower.getScentId(), flower.getBlockId());
                continue;
            }

            // Flowers are blocks at runtime
            BlockTriggerDefinition trigger = new BlockTriggerDefinition();
            trigger.setBlockId(flower.getBlockId());
            trigger.setScentName(scentName);
            trigger.setTriggerOn(flower.getTriggerOn());
            trigger.setRange(flower.getRange());
            trigger.setPriority(flower.getPriority());
            trigger.setIntensity(flower.getIntensity());

            validateScentName(scentName, "flower", flower.getBlockId());
            blockTriggerMap.put(flower.getBlockId(), trigger);
            AromaAffect.LOGGER.debug("Registered flower trigger: {} -> {}",
                    flower.getBlockId(), scentName);
        }
    }

    /**
     * Builds structure trigger map from StructureDefinitionLoader.
     */
    private static void buildStructureTriggers() {
        structureTriggerMap.clear();
        for (StructureDefinition structure : StructureDefinitionLoader.getLoadedStructures()) {
            if (!structure.hasScentId()) continue;

            String scentName = resolveScentName(structure.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) {
                AromaAffect.LOGGER.warn("Could not resolve scent_id '{}' for structure '{}'",
                        structure.getScentId(), structure.getStructureId());
                continue;
            }

            StructureTriggerDefinition trigger = new StructureTriggerDefinition();
            trigger.setStructureId(structure.getStructureId());
            trigger.setScentName(scentName);
            trigger.setMode(structure.getMode());
            trigger.setRange(structure.getRange());
            trigger.setPriority(structure.getPriority());
            trigger.setIntensity(structure.getIntensity());

            validateScentName(scentName, "structure", structure.getStructureId());
            structureTriggerMap.put(structure.getStructureId(), trigger);
            AromaAffect.LOGGER.debug("Registered structure trigger: {} -> {}",
                    structure.getStructureId(), scentName);
        }
    }

    /**
     * Builds mob trigger map from MobDefinitionLoader.
     */
    private static void buildMobTriggers() {
        mobTriggerMap.clear();
        for (MobDefinition mob : MobDefinitionLoader.getLoadedMobs()) {
            if (!mob.hasScentId()) continue;

            String scentName = resolveScentName(mob.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) {
                AromaAffect.LOGGER.warn("Could not resolve scent_id '{}' for mob '{}'",
                        mob.getScentId(), mob.getEntityType());
                continue;
            }

            MobTriggerDefinition trigger = new MobTriggerDefinition();
            trigger.setEntityType(mob.getEntityType());
            trigger.setScentName(scentName);
            trigger.setRange(mob.getRange());
            trigger.setPriority(mob.getPriority());
            trigger.setIntensity(mob.getIntensity());

            validateScentName(scentName, "mob", mob.getEntityType());
            mobTriggerMap.put(mob.getEntityType(), trigger);
            AromaAffect.LOGGER.debug("Registered mob trigger: {} -> {}",
                    mob.getEntityType(), scentName);
        }
    }

    /**
     * Validates that a scent name is in the list of valid OVR scents.
     */
    private static void validateScentName(String scentName, String triggerType, String triggerId) {
        if (!VALID_OVR_SCENTS.contains(scentName)) {
            AromaAffect.LOGGER.warn("Unknown OVR scent '{}' in {} trigger '{}'. " +
                    "This scent may not be supported by the hardware.",
                    scentName, triggerType, triggerId);
        }
    }

    // ========================================
    // Lookup Methods
    // ========================================

    public static Optional<ItemTriggerDefinition> getItemTrigger(String itemId) {
        return Optional.ofNullable(itemTriggerMap.get(itemId));
    }

    public static Optional<BiomeTriggerDefinition> getBiomeTrigger(String biomeId) {
        return Optional.ofNullable(biomeTriggerMap.get(biomeId));
    }

    public static Optional<BlockTriggerDefinition> getBlockTrigger(String blockId) {
        return Optional.ofNullable(blockTriggerMap.get(blockId));
    }

    public static Optional<MobTriggerDefinition> getMobTrigger(String entityType) {
        return Optional.ofNullable(mobTriggerMap.get(entityType));
    }

    public static boolean hasItemTrigger(String itemId) {
        return itemTriggerMap.containsKey(itemId);
    }

    public static Collection<ItemTriggerDefinition> getAllItemTriggers() {
        return Collections.unmodifiableCollection(itemTriggerMap.values());
    }

    public static Collection<BlockTriggerDefinition> getAllBlockTriggers() {
        return Collections.unmodifiableCollection(blockTriggerMap.values());
    }

    public static Collection<BiomeTriggerDefinition> getAllBiomeTriggers() {
        return Collections.unmodifiableCollection(biomeTriggerMap.values());
    }

    public static Optional<StructureTriggerDefinition> getStructureTrigger(String structureId) {
        return Optional.ofNullable(structureTriggerMap.get(structureId));
    }

    public static Collection<StructureTriggerDefinition> getAllStructureTriggers() {
        return Collections.unmodifiableCollection(structureTriggerMap.values());
    }

    public static Collection<MobTriggerDefinition> getAllMobTriggers() {
        return Collections.unmodifiableCollection(mobTriggerMap.values());
    }

    public static TriggerSettings getSettings() {
        return settings != null ? settings : TriggerSettings.defaults();
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void reload() {
        AromaAffect.LOGGER.info("Reloading scent trigger configuration...");
        initialized = false;
        init();
    }

    /**
     * Internal root class for loading item triggers from scent_items.json.
     */
    private static class ItemTriggersRoot {
        @com.google.gson.annotations.SerializedName("item_triggers")
        private List<ItemTriggerDefinition> itemTriggers = new ArrayList<>();

        public List<ItemTriggerDefinition> getItemTriggers() {
            return itemTriggers != null ? itemTriggers : new ArrayList<>();
        }
    }
}
