package com.ovrtechnology.structure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.block.BlockRegistry;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.scent.ScentRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

public class StructureDefinitionLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String STRUCTURES_DIR = "aroma_structures";

    @Getter private static List<StructureDefinition> loadedStructures = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    @Getter private static List<String> validationWarnings = new ArrayList<>();

    public static List<StructureDefinition> loadAllStructures() {
        return loadAllStructures(ClasspathDataSource.INSTANCE);
    }

    public static List<StructureDefinition> loadAllStructures(DataSource dataSource) {
        loadedStructures.clear();
        loadedIds.clear();
        validationWarnings.clear();

        Map<ResourceLocation, JsonElement> files = dataSource.listJson(STRUCTURES_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                StructureDefinition structure =
                        GSON.fromJson(entry.getValue(), StructureDefinition.class);
                processStructure(structure);
            } catch (Exception e) {
                AromaAffect.LOGGER.error(
                        "Failed to parse structure {}: {}", entry.getKey(), e.getMessage());
            }
        }

        AromaAffect.LOGGER.info(
                "Loaded {} structure definitions from {} file(s)",
                loadedStructures.size(),
                files.size());

        if (!validationWarnings.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "Structure loading completed with {} validation warnings",
                    validationWarnings.size());
        }

        return Collections.unmodifiableList(loadedStructures);
    }

    private static void processStructure(StructureDefinition structure) {
        if (structure == null) {
            addWarning("Null structure definition found, skipping...");
            return;
        }

        if (!structure.isValid()) {
            addWarning("Invalid structure definition found (missing structure_id), skipping...");
            return;
        }

        String structureId = structure.getStructureId();

        if (loadedIds.contains(structureId)) {
            addWarning("Duplicate structure_id '" + structureId + "' found, skipping...");
            return;
        }

        validateStructure(structure);

        loadedIds.add(structureId);
        loadedStructures.add(structure);
        AromaAffect.LOGGER.debug(
                "Loaded structure definition: {} (color: {}, scent: {})",
                structureId,
                structure.getColorHtml(),
                structure.getScentId());
    }

    private static void validateStructure(StructureDefinition structure) {
        String structureId = structure.getStructureId();

        if (!structure.hasValidStructureIdFormat()) {
            addWarning(
                    "["
                            + structureId
                            + "] Invalid structure_id format - should be 'namespace:path' (e.g., 'minecraft:stronghold')");
        }

        String rawColor = structure.getRawColorHtml();
        if (rawColor == null || rawColor.isEmpty()) {
            addWarning(
                    "["
                            + structureId
                            + "] No color_html defined, using default: "
                            + StructureDefinition.DEFAULT_COLOR);
        } else if (!StructureDefinition.isValidHtmlColor(rawColor)) {
            addWarning(
                    "["
                            + structureId
                            + "] Invalid color_html format '"
                            + rawColor
                            + "', using default: "
                            + StructureDefinition.DEFAULT_COLOR);
        }

        if (structure.hasScentId()) {
            String scentId = structure.getScentId();
            if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(scentId)) {
                addWarning(
                        "["
                                + structureId
                                + "] Referenced scent_id '"
                                + scentId
                                + "' does not exist in ScentRegistry");
            }
        } else {
            addWarning(
                    "["
                            + structureId
                            + "] No scent_id defined, structure will have no associated scent");
        }

        if (structure.hasBlocks()) {
            validateBlockReferences(structureId, structure.getBlocks());
        }

        String rawImage = structure.getRawImage();
        if (rawImage == null || rawImage.isEmpty()) {
            AromaAffect.LOGGER.debug(
                    "[{}] No image defined, using default: {}",
                    structureId,
                    StructureDefinition.DEFAULT_IMAGE);
        }
    }

    private static void validateBlockReferences(String structureId, List<String> blockIds) {
        if (!BlockRegistry.isInitialized()) {
            AromaAffect.LOGGER.debug(
                    "[{}] BlockRegistry not initialized, skipping block validation", structureId);
            return;
        }

        List<String> invalidBlocks = BlockRegistry.validateBlockIds(blockIds);
        for (String invalidBlock : invalidBlocks) {

            AromaAffect.LOGGER.debug(
                    "[{}] Block '{}' not found in BlockRegistry (may still be valid Minecraft block)",
                    structureId,
                    invalidBlock);
        }

        for (String blockId : blockIds) {
            if (!StructureDefinition.isValidResourceLocation(blockId)) {
                addWarning(
                        "["
                                + structureId
                                + "] Block ID '"
                                + blockId
                                + "' has invalid format - should be 'namespace:path'");
            }
        }
    }

    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaAffect.LOGGER.warn(warning);
    }

    public static StructureDefinition getStructureById(String structureId) {
        for (StructureDefinition structure : loadedStructures) {
            if (structure.getStructureId().equals(structureId)) {
                return structure;
            }
        }
        return null;
    }

    public static boolean hasStructureId(String structureId) {
        return loadedIds.contains(structureId);
    }

    public static List<StructureDefinition> getVanillaStructures() {
        List<StructureDefinition> result = new ArrayList<>();
        for (StructureDefinition structure : loadedStructures) {
            if (structure.isVanilla()) {
                result.add(structure);
            }
        }
        return result;
    }

    public static List<StructureDefinition> getModdedStructures() {
        List<StructureDefinition> result = new ArrayList<>();
        for (StructureDefinition structure : loadedStructures) {
            if (!structure.isVanilla()) {
                result.add(structure);
            }
        }
        return result;
    }

    public static String toJson(StructureDefinition structure) {
        return GSON.toJson(structure);
    }

    public static Gson getGson() {
        return GSON;
    }

    public static List<StructureDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading structure definitions...");
        return loadAllStructures();
    }

    public static boolean hasValidationWarnings() {
        return !validationWarnings.isEmpty();
    }
}
