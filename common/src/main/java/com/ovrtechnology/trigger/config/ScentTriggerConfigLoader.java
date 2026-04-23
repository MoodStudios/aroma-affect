package com.ovrtechnology.trigger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.biome.BiomeDefinition;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
import com.ovrtechnology.block.BlockDefinition;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.flower.FlowerDefinition;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.mob.MobDefinition;
import com.ovrtechnology.mob.MobDefinitionLoader;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.structure.StructureDefinition;
import com.ovrtechnology.structure.StructureDefinitionLoader;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

public final class ScentTriggerConfigLoader {

    public static final String ITEM_TRIGGERS_DIR = "aroma_scent_triggers";

    private static final String SETTINGS_PATH = "data/aromaaffect/scents/trigger_settings.json";

    private static final String OVR_CATALOG_PATH = "data/aromaaffect/scents/ovr_catalog.json";

    private static final Set<String> DEFAULT_OVR_SCENTS =
            Set.of(
                    "Beach",
                    "Evergreen",
                    "Desert",
                    "Floral",
                    "Barnyard",
                    "Smoky",
                    "Winter",
                    "Terra Silva",
                    "Savory Spice",
                    "Timber",
                    "Petrichor",
                    "Sweet",
                    "Machina",
                    "Marine",
                    "Kindred",
                    "Citrus");

    public static Set<String> VALID_OVR_SCENTS = DEFAULT_OVR_SCENTS;

    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static TriggerSettings settings;

    private static final Map<String, ItemTriggerDefinition> itemTriggerMap = new HashMap<>();
    private static final Map<String, BiomeTriggerDefinition> biomeTriggerMap = new HashMap<>();
    private static final Map<String, BlockTriggerDefinition> blockTriggerMap = new HashMap<>();
    private static final Map<String, MobTriggerDefinition> mobTriggerMap = new HashMap<>();
    private static final Map<String, StructureTriggerDefinition> structureTriggerMap =
            new HashMap<>();

    private static boolean initialized = false;

    private ScentTriggerConfigLoader() {}

    public static void init() {
        init(ClasspathDataSource.INSTANCE);
    }

    public static void init(DataSource dataSource) {
        if (initialized) {
            AromaAffect.LOGGER.warn("ScentTriggerConfigLoader.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Loading scent trigger configuration...");

        try {
            loadOvrCatalog(dataSource);

            settings = loadSettings(dataSource);
            if (settings == null) {
                settings = TriggerSettings.defaults();
            }
            settings.validate();

            loadItemTriggers(dataSource);

            buildBiomeTriggers();
            buildBlockTriggers();
            buildFlowerTriggers();
            buildStructureTriggers();
            buildMobTriggers();

            int total =
                    itemTriggerMap.size()
                            + biomeTriggerMap.size()
                            + blockTriggerMap.size()
                            + mobTriggerMap.size()
                            + structureTriggerMap.size();

            AromaAffect.LOGGER.info(
                    "Loaded {} scent triggers ({} item, {} biome, {} block, {} mob, {} structure)",
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

    private static void loadOvrCatalog(DataSource dataSource) {
        JsonElement element = dataSource.read(OVR_CATALOG_PATH);
        if (element == null) {
            VALID_OVR_SCENTS = DEFAULT_OVR_SCENTS;
            return;
        }
        try {
            Set<String> parsed = new LinkedHashSet<>();
            if (element.isJsonArray()) {
                element.getAsJsonArray().forEach(e -> parsed.add(e.getAsString()));
            } else if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("scents") && obj.get("scents").isJsonArray()) {
                    obj.getAsJsonArray("scents").forEach(e -> parsed.add(e.getAsString()));
                }
            }
            VALID_OVR_SCENTS =
                    parsed.isEmpty() ? DEFAULT_OVR_SCENTS : Collections.unmodifiableSet(parsed);
            AromaAffect.LOGGER.info("Loaded {} OVR catalog entries", VALID_OVR_SCENTS.size());
        } catch (Exception e) {
            AromaAffect.LOGGER.error(
                    "Failed to parse OVR catalog at {}, using defaults: {}",
                    OVR_CATALOG_PATH,
                    e.getMessage());
            VALID_OVR_SCENTS = DEFAULT_OVR_SCENTS;
        }
    }

    private static TriggerSettings loadSettings(DataSource dataSource) {
        JsonElement element = dataSource.read(SETTINGS_PATH);
        if (element == null) {
            AromaAffect.LOGGER.warn(
                    "Trigger settings not found: {}, using defaults", SETTINGS_PATH);
            return null;
        }
        try {
            return GSON.fromJson(element, TriggerSettings.class);
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error reading trigger settings: {}", e.getMessage());
            return null;
        }
    }

    private static void loadItemTriggers(DataSource dataSource) {
        itemTriggerMap.clear();
        Map<ResourceLocation, JsonElement> files = dataSource.listJson(ITEM_TRIGGERS_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                ItemTriggerDefinition trigger =
                        GSON.fromJson(entry.getValue(), ItemTriggerDefinition.class);
                if (trigger != null && trigger.isValid()) {
                    validateScentName(trigger.getScentName(), "item", trigger.getItemId());
                    itemTriggerMap.put(trigger.getItemId(), trigger);
                }
            } catch (Exception e) {
                AromaAffect.LOGGER.error(
                        "Failed to parse scent trigger {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    private static String resolveScentName(String scentId) {
        if (scentId == null || scentId.isEmpty()) {
            return null;
        }
        return ScentRegistry.getDisplayName(scentId);
    }

    private static void buildBiomeTriggers() {
        biomeTriggerMap.clear();
        for (BiomeDefinition biome : BiomeDefinitionLoader.getLoadedBiomes()) {
            if (!biome.hasScentId()) continue;

            String scentName = resolveScentName(biome.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) {
                AromaAffect.LOGGER.warn(
                        "Could not resolve scent_id '{}' for biome '{}'",
                        biome.getScentId(),
                        biome.getBiomeId());
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
            AromaAffect.LOGGER.debug(
                    "Registered biome trigger: {} -> {}", biome.getBiomeId(), scentName);
        }
    }

    private static void buildBlockTriggers() {
        blockTriggerMap.clear();
        for (BlockDefinition block : BlockDefinitionLoader.getLoadedBlocks()) {
            if (!block.hasScentId()) continue;

            String scentName = resolveScentName(block.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) {
                AromaAffect.LOGGER.warn(
                        "Could not resolve scent_id '{}' for block '{}'",
                        block.getScentId(),
                        block.getBlockId());
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
            AromaAffect.LOGGER.debug(
                    "Registered block trigger: {} -> {}", block.getBlockId(), scentName);
        }
    }

    private static void buildFlowerTriggers() {
        for (FlowerDefinition flower : FlowerDefinitionLoader.getLoadedFlowers()) {
            if (!flower.hasScentId()) continue;

            String scentName = resolveScentName(flower.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) {
                AromaAffect.LOGGER.warn(
                        "Could not resolve scent_id '{}' for flower '{}'",
                        flower.getScentId(),
                        flower.getBlockId());
                continue;
            }

            BlockTriggerDefinition trigger = new BlockTriggerDefinition();
            trigger.setBlockId(flower.getBlockId());
            trigger.setScentName(scentName);
            trigger.setTriggerOn(flower.getTriggerOn());
            trigger.setRange(flower.getRange());
            trigger.setPriority(flower.getPriority());
            trigger.setIntensity(flower.getIntensity());

            validateScentName(scentName, "flower", flower.getBlockId());
            blockTriggerMap.put(flower.getBlockId(), trigger);
            AromaAffect.LOGGER.debug(
                    "Registered flower trigger: {} -> {}", flower.getBlockId(), scentName);
        }
    }

    private static void buildStructureTriggers() {
        structureTriggerMap.clear();
        for (StructureDefinition structure : StructureDefinitionLoader.getLoadedStructures()) {
            if (!structure.hasScentId()) continue;

            String scentName = resolveScentName(structure.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) {
                AromaAffect.LOGGER.warn(
                        "Could not resolve scent_id '{}' for structure '{}'",
                        structure.getScentId(),
                        structure.getStructureId());
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
            AromaAffect.LOGGER.debug(
                    "Registered structure trigger: {} -> {}",
                    structure.getStructureId(),
                    scentName);
        }
    }

    private static void buildMobTriggers() {
        mobTriggerMap.clear();
        for (MobDefinition mob : MobDefinitionLoader.getLoadedMobs()) {
            if (!mob.hasScentId()) continue;

            String scentName = resolveScentName(mob.getScentId());
            if (scentName == null || "Unknown Scent".equals(scentName)) {
                AromaAffect.LOGGER.warn(
                        "Could not resolve scent_id '{}' for mob '{}'",
                        mob.getScentId(),
                        mob.getEntityType());
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
            AromaAffect.LOGGER.debug(
                    "Registered mob trigger: {} -> {}", mob.getEntityType(), scentName);
        }
    }

    private static void validateScentName(String scentName, String triggerType, String triggerId) {
        if (!VALID_OVR_SCENTS.contains(scentName)) {
            AromaAffect.LOGGER.warn(
                    "Unknown OVR scent '{}' in {} trigger '{}'. "
                            + "This scent may not be supported by the hardware.",
                    scentName,
                    triggerType,
                    triggerId);
        }
    }

    public static Optional<ItemTriggerDefinition> getItemTrigger(String itemId) {
        return Optional.ofNullable(itemTriggerMap.get(itemId));
    }

    public static Optional<BiomeTriggerDefinition> getBiomeTrigger(String biomeId) {
        return Optional.ofNullable(biomeTriggerMap.get(biomeId));
    }

    public static Optional<BlockTriggerDefinition> getBlockTrigger(String blockId) {
        return Optional.ofNullable(blockTriggerMap.get(blockId));
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
        reload(ClasspathDataSource.INSTANCE);
    }

    public static void reload(DataSource dataSource) {
        AromaAffect.LOGGER.info("Reloading scent trigger configuration...");
        initialized = false;
        init(dataSource);
    }
}
