package com.ovrtechnology.nose;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.ability.AbilityDefinitionLoader;
import com.ovrtechnology.biome.BiomeRegistry;
import com.ovrtechnology.block.BlockRegistry;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.data.JsonResources;
import com.ovrtechnology.structure.StructureRegistry;
import lombok.Getter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads nose definitions from JSON files.
 * Parses the data/aromaaffect/noses/ directory for nose definitions.
 * Handles texture validation and fallback to foragers_nose texture.
 * 
 * Note: Recipes are defined separately in data/aromaaffect/recipe/ as standard Minecraft recipe JSON files.
 */
public class NoseDefinitionLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Default texture path for fallback
     */
    private static final String DEFAULT_TEXTURE = "item/foragers_nose";
    
    /**
     * Default model (iron helmet) for fallback
     */
    private static final String DEFAULT_MODEL = "minecraft:iron_helmet";
    
    /**
     * Cached list of loaded nose definitions
     */
    @Getter
    private static List<NoseDefinition> loadedNoses = new ArrayList<>();
    
    /**
     * Load all nose definitions from the noses directory.
     * This scans for JSON files and parses each one.
     */
    public static List<NoseDefinition> loadAllNoses() {
        return loadAllNoses(ClasspathDataSource.INSTANCE);
    }

    public static List<NoseDefinition> loadAllNoses(DataSource dataSource) {
        loadedNoses.clear();

        try {
            NoseDefinition[] noses = loadNosesFromResource(dataSource, "data/aromaaffect/noses/noses.json");
            if (noses != null) {
                for (NoseDefinition nose : noses) {
                    if (nose != null && nose.isValid()) {
                        validateAndApplyFallbacks(nose);

                        loadedNoses.add(nose);
                        AromaAffect.LOGGER.info("Loaded nose definition: {}", nose.getId());
                    } else {
                        AromaAffect.LOGGER.warn("Invalid nose definition found, skipping...");
                    }
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load nose definitions", e);
        }

        AromaAffect.LOGGER.info("Loaded {} nose definitions", loadedNoses.size());
        return Collections.unmodifiableList(loadedNoses);
    }

    /**
     * Reload-friendly variant used by the datapack reload listener. Instead of
     * replacing {@link NoseDefinition} instances, this mutates the fields of
     * the existing instances in place — the {@code NoseItem} objects registered
     * at mod-init hold those references, so mutable fields (abilities, unlock,
     * image, tier/track_cost for gameplay logic, enabled) propagate to items
     * immediately. Item properties baked at construction (durability cap,
     * rarity, model equipment asset, repair component, stack size) remain
     * fixed — those need a game restart to change.
     *
     * <p>Slot entries are reset to their registration defaults BEFORE applying
     * JSON, so removing a datapack reverts slots to disabled on the next
     * {@code /reload}. Built-in noses re-read their defaults from the mod's
     * own {@code noses.json} (classpath layer under any datapack override).</p>
     *
     * <p>Entries in JSON with IDs that aren't pre-registered are ignored with
     * a warning — point modpack makers at {@link com.ovrtechnology.slots.SlotPool}.</p>
     */
    public static void reloadInPlace(DataSource dataSource) {
        NoseDefinition[] newDefs = loadNosesFromResource(dataSource, "data/aromaaffect/noses/noses.json");
        int mutated = 0;
        int skipped = 0;
        for (NoseDefinition src : newDefs) {
            if (src == null || !src.isValid()) continue;
            NoseDefinition target = findLoadedById(src.getId());
            if (target == null) {
                AromaAffect.LOGGER.warn(
                        "[Nose reload] ID '{}' is not a built-in nose; custom noses must use the aromaaffect:custom_nose variant system.",
                        src.getId());
                skipped++;
                continue;
            }
            copyFields(src, target);
            validateAndApplyFallbacks(target);
            mutated++;
        }
        AromaAffect.LOGGER.info("Nose reload: mutated {} definitions in place, skipped {} unknown IDs",
                mutated, skipped);
    }

    private static NoseDefinition findLoadedById(String id) {
        for (NoseDefinition d : loadedNoses) {
            if (id.equals(d.getId())) return d;
        }
        return null;
    }

    private static void copyFields(NoseDefinition src, NoseDefinition dst) {
        dst.setImage(src.getImage());
        dst.setModel(src.getModel());
        dst.setUnlock(src.getUnlock());
        dst.setDurability(src.getDurability());
        dst.setRepair(src.getRepair());
        dst.setTier(src.getTier());
        dst.setTrackCost(src.getTrackCost());
        dst.setEnabled(src.isEnabled());
    }
    
    /**
     * Validate nose definition and apply fallbacks for missing textures/models
     */
    private static void validateAndApplyFallbacks(NoseDefinition nose) {
        String noseId = nose.getId();
        
        // Check texture
        String texturePath = nose.getImage();
        if (texturePath == null || texturePath.isEmpty()) {
            AromaAffect.LOGGER.warn("[{}] No texture defined, using fallback: {}", noseId, DEFAULT_TEXTURE);
            nose.setImage(DEFAULT_TEXTURE);
        } else if (!textureExists(texturePath)) {
            AromaAffect.LOGGER.warn("[{}] Texture '{}' not found, using fallback: {}", noseId, texturePath, DEFAULT_TEXTURE);
            nose.setImage(DEFAULT_TEXTURE);
        }
        
        // Check model - default to iron_helmet if not specified
        String model = nose.getModel();
        if (model == null || model.isEmpty()) {
            AromaAffect.LOGGER.info("[{}] No model defined, using default: {}", noseId, DEFAULT_MODEL);
        }
        
        // Validate unlock references
        NoseUnlock unlock = nose.getUnlock();
        if (unlock != null) {
            // Validate nose inheritance
            if (unlock.hasNoseInheritance()) {
                for (String inheritedNoseId : unlock.getNoses()) {
                    AromaAffect.LOGGER.debug("[{}] Inherits abilities from: {}", noseId, inheritedNoseId);
                }
            }
            
            // Validate and filter block references against BlockRegistry
            if (unlock.hasBlockUnlocks() && BlockRegistry.isInitialized()) {
                List<String> invalidBlocks = BlockRegistry.validateBlockIds(unlock.getBlocks());
                if (!invalidBlocks.isEmpty()) {
                    for (String invalidBlock : invalidBlocks) {
                        AromaAffect.LOGGER.warn("[{}] Skipping unregistered block: '{}' - block must be defined in blocks.json", 
                                noseId, invalidBlock);
                    }
                    // Filter out invalid blocks
                    List<String> validBlocks = new ArrayList<>(unlock.getBlocks());
                    validBlocks.removeAll(invalidBlocks);
                    unlock.setBlocks(validBlocks);
                    AromaAffect.LOGGER.info("[{}] Kept {} valid block references after filtering", noseId, validBlocks.size());
                }
            }
            
            // Validate and filter structure references against StructureRegistry
            if (unlock.hasStructureUnlocks() && StructureRegistry.isInitialized()) {
                List<String> invalidStructures = StructureRegistry.validateStructureIds(unlock.getStructures());
                if (!invalidStructures.isEmpty()) {
                    for (String invalidStructure : invalidStructures) {
                        AromaAffect.LOGGER.warn("[{}] Skipping unregistered structure: '{}' - structure must be defined in structures.json", 
                                noseId, invalidStructure);
                    }
                    // Filter out invalid structures
                    List<String> validStructures = new ArrayList<>(unlock.getStructures());
                    validStructures.removeAll(invalidStructures);
                    unlock.setStructures(validStructures);
                    AromaAffect.LOGGER.info("[{}] Kept {} valid structure references after filtering", noseId, validStructures.size());
                }
            }
            
            // Validate and filter biome references against BiomeRegistry
            if (unlock.hasBiomeUnlocks() && BiomeRegistry.isInitialized()) {
                List<String> invalidBiomes = BiomeRegistry.validateBiomeIds(unlock.getBiomes());
                if (!invalidBiomes.isEmpty()) {
                    for (String invalidBiome : invalidBiomes) {
                        AromaAffect.LOGGER.warn("[{}] Skipping unregistered biome: '{}' - biome must be defined in biomes.json", 
                                noseId, invalidBiome);
                    }
                    // Filter out invalid biomes
                    List<String> validBiomes = new ArrayList<>(unlock.getBiomes());
                    validBiomes.removeAll(invalidBiomes);
                    unlock.setBiomes(validBiomes);
                    AromaAffect.LOGGER.info("[{}] Kept {} valid biome references after filtering", noseId, validBiomes.size());
                }
            }
            
            // Validate and filter ability references against AbilityDefinitionLoader
            if (unlock.hasAbilityUnlocks() && AbilityDefinitionLoader.isInitialized()) {
                List<String> invalidAbilities = AbilityDefinitionLoader.validateAbilityIds(unlock.getAbilities());
                if (!invalidAbilities.isEmpty()) {
                    for (String invalidAbility : invalidAbilities) {
                        AromaAffect.LOGGER.warn("[{}] Skipping unregistered ability: '{}' - ability must be defined in abilities.json", 
                                noseId, invalidAbility);
                    }
                    // Filter out invalid abilities
                    List<String> validAbilities = new ArrayList<>(unlock.getAbilities());
                    validAbilities.removeAll(invalidAbilities);
                    unlock.setAbilities(validAbilities);
                    AromaAffect.LOGGER.info("[{}] Kept {} valid ability references after filtering", noseId, validAbilities.size());
                }
            }
        }
    }
    
    /**
     * Check if a texture file exists in the resources
     */
    private static boolean textureExists(String texturePath) {
        // Convert texture path to full resource path
        // texturePath is like "item/foragers_nose", full path is "assets/aromaaffect/textures/item/foragers_nose.png"
        String fullPath = "assets/aromaaffect/textures/" + texturePath + ".png";
        
        try (InputStream stream = NoseDefinitionLoader.class.getClassLoader().getResourceAsStream(fullPath)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static NoseDefinition[] loadNosesFromResource(DataSource dataSource, String resourcePath) {
        JsonElement element = dataSource.read(resourcePath);
        if (element == null) {
            AromaAffect.LOGGER.warn("Nose definitions file not found: {}", resourcePath);
            return new NoseDefinition[0];
        }
        return JsonResources.parseArrayOrWrapped(element, "noses", NoseDefinition[].class, GSON, resourcePath);
    }
    
    /**
     * Parse a single nose definition from a JSON string
     */
    public static NoseDefinition parseNoseFromJson(String json) {
        try {
            return GSON.fromJson(json, NoseDefinition.class);
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to parse nose definition from JSON", e);
            return null;
        }
    }
    
    /**
     * Get a nose definition by ID
     */
    public static NoseDefinition getNoseById(String id) {
        for (NoseDefinition nose : loadedNoses) {
            if (nose.getId().equals(id)) {
                return nose;
            }
        }
        return null;
    }
    
    /**
     * Get all nose definitions for a specific tier
     */
    public static List<NoseDefinition> getNosesByTier(int tier) {
        List<NoseDefinition> result = new ArrayList<>();
        for (NoseDefinition nose : loadedNoses) {
            if (nose.getTier() == tier) {
                result.add(nose);
            }
        }
        return result;
    }
    
    /**
     * Serialize a nose definition to JSON (useful for debugging/export)
     */
    public static String toJson(NoseDefinition nose) {
        return GSON.toJson(nose);
    }
    
    /**
     * Get the GSON instance for external use
     */
    public static Gson getGson() {
        return GSON;
    }
}
