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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

public class NoseDefinitionLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String DEFAULT_TEXTURE = "item/foragers_nose";

    private static final String DEFAULT_MODEL = "minecraft:iron_helmet";

    @Getter private static List<NoseDefinition> loadedNoses = new ArrayList<>();

    public static List<NoseDefinition> loadAllNoses() {
        return loadAllNoses(ClasspathDataSource.INSTANCE);
    }

    public static List<NoseDefinition> loadAllNoses(DataSource dataSource) {
        loadedNoses.clear();

        try {
            NoseDefinition[] noses =
                    loadNosesFromResource(dataSource, "data/aromaaffect/noses/noses.json");
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

    public static void reloadInPlace(DataSource dataSource) {
        NoseDefinition[] newDefs =
                loadNosesFromResource(dataSource, "data/aromaaffect/noses/noses.json");
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
        AromaAffect.LOGGER.info(
                "Nose reload: mutated {} definitions in place, skipped {} unknown IDs",
                mutated,
                skipped);
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

    private static void validateAndApplyFallbacks(NoseDefinition nose) {
        String noseId = nose.getId();

        String texturePath = nose.getImage();
        if (texturePath == null || texturePath.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "[{}] No texture defined, using fallback: {}", noseId, DEFAULT_TEXTURE);
            nose.setImage(DEFAULT_TEXTURE);
        } else if (!textureExists(texturePath)) {
            AromaAffect.LOGGER.warn(
                    "[{}] Texture '{}' not found, using fallback: {}",
                    noseId,
                    texturePath,
                    DEFAULT_TEXTURE);
            nose.setImage(DEFAULT_TEXTURE);
        }

        String model = nose.getModel();
        if (model == null || model.isEmpty()) {
            AromaAffect.LOGGER.info(
                    "[{}] No model defined, using default: {}", noseId, DEFAULT_MODEL);
        }

        NoseUnlock unlock = nose.getUnlock();
        if (unlock != null) {

            if (unlock.hasNoseInheritance()) {
                for (String inheritedNoseId : unlock.getNoses()) {
                    AromaAffect.LOGGER.debug(
                            "[{}] Inherits abilities from: {}", noseId, inheritedNoseId);
                }
            }

            if (unlock.hasBlockUnlocks() && BlockRegistry.isInitialized()) {
                List<String> invalidBlocks = BlockRegistry.validateBlockIds(unlock.getBlocks());
                if (!invalidBlocks.isEmpty()) {
                    for (String invalidBlock : invalidBlocks) {
                        AromaAffect.LOGGER.warn(
                                "[{}] Skipping unregistered block: '{}' - block must be defined in blocks.json",
                                noseId,
                                invalidBlock);
                    }

                    List<String> validBlocks = new ArrayList<>(unlock.getBlocks());
                    validBlocks.removeAll(invalidBlocks);
                    unlock.setBlocks(validBlocks);
                    AromaAffect.LOGGER.info(
                            "[{}] Kept {} valid block references after filtering",
                            noseId,
                            validBlocks.size());
                }
            }

            if (unlock.hasStructureUnlocks() && StructureRegistry.isInitialized()) {
                List<String> invalidStructures =
                        StructureRegistry.validateStructureIds(unlock.getStructures());
                if (!invalidStructures.isEmpty()) {
                    for (String invalidStructure : invalidStructures) {
                        AromaAffect.LOGGER.warn(
                                "[{}] Skipping unregistered structure: '{}' - structure must be defined in structures.json",
                                noseId,
                                invalidStructure);
                    }

                    List<String> validStructures = new ArrayList<>(unlock.getStructures());
                    validStructures.removeAll(invalidStructures);
                    unlock.setStructures(validStructures);
                    AromaAffect.LOGGER.info(
                            "[{}] Kept {} valid structure references after filtering",
                            noseId,
                            validStructures.size());
                }
            }

            if (unlock.hasBiomeUnlocks() && BiomeRegistry.isInitialized()) {
                List<String> invalidBiomes = BiomeRegistry.validateBiomeIds(unlock.getBiomes());
                if (!invalidBiomes.isEmpty()) {
                    for (String invalidBiome : invalidBiomes) {
                        AromaAffect.LOGGER.warn(
                                "[{}] Skipping unregistered biome: '{}' - biome must be defined in biomes.json",
                                noseId,
                                invalidBiome);
                    }

                    List<String> validBiomes = new ArrayList<>(unlock.getBiomes());
                    validBiomes.removeAll(invalidBiomes);
                    unlock.setBiomes(validBiomes);
                    AromaAffect.LOGGER.info(
                            "[{}] Kept {} valid biome references after filtering",
                            noseId,
                            validBiomes.size());
                }
            }

            if (unlock.hasAbilityUnlocks() && AbilityDefinitionLoader.isInitialized()) {
                List<String> invalidAbilities =
                        AbilityDefinitionLoader.validateAbilityIds(unlock.getAbilities());
                if (!invalidAbilities.isEmpty()) {
                    for (String invalidAbility : invalidAbilities) {
                        AromaAffect.LOGGER.warn(
                                "[{}] Skipping unregistered ability: '{}' - ability must be defined in abilities.json",
                                noseId,
                                invalidAbility);
                    }

                    List<String> validAbilities = new ArrayList<>(unlock.getAbilities());
                    validAbilities.removeAll(invalidAbilities);
                    unlock.setAbilities(validAbilities);
                    AromaAffect.LOGGER.info(
                            "[{}] Kept {} valid ability references after filtering",
                            noseId,
                            validAbilities.size());
                }
            }
        }
    }

    private static boolean textureExists(String texturePath) {

        String fullPath = "assets/aromaaffect/textures/" + texturePath + ".png";

        try (InputStream stream =
                NoseDefinitionLoader.class.getClassLoader().getResourceAsStream(fullPath)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static NoseDefinition[] loadNosesFromResource(
            DataSource dataSource, String resourcePath) {
        JsonElement element = dataSource.read(resourcePath);
        if (element == null) {
            AromaAffect.LOGGER.warn("Nose definitions file not found: {}", resourcePath);
            return new NoseDefinition[0];
        }
        return JsonResources.parseArrayOrWrapped(
                element, "noses", NoseDefinition[].class, GSON, resourcePath);
    }

    public static String toJson(NoseDefinition nose) {
        return GSON.toJson(nose);
    }

    public static Gson getGson() {
        return GSON;
    }
}
