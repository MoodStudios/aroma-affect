package com.ovrtechnology.flower;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
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

public class FlowerDefinitionLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String FLOWERS_DIR = "aroma_flowers";

    @Getter private static List<FlowerDefinition> loadedFlowers = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    @Getter private static List<String> validationWarnings = new ArrayList<>();

    public static List<FlowerDefinition> loadAllFlowers() {
        return loadAllFlowers(ClasspathDataSource.INSTANCE);
    }

    public static List<FlowerDefinition> loadAllFlowers(DataSource dataSource) {
        loadedFlowers.clear();
        loadedIds.clear();
        validationWarnings.clear();

        Map<ResourceLocation, JsonElement> files = dataSource.listJson(FLOWERS_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                FlowerDefinition flower = GSON.fromJson(entry.getValue(), FlowerDefinition.class);
                processFlower(flower);
            } catch (Exception e) {
                AromaAffect.LOGGER.error(
                        "Failed to parse flower {}: {}", entry.getKey(), e.getMessage());
            }
        }

        AromaAffect.LOGGER.info(
                "Loaded {} flower definitions from {} file(s)", loadedFlowers.size(), files.size());

        if (!validationWarnings.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "Flower loading completed with {} validation warnings",
                    validationWarnings.size());
        }

        return Collections.unmodifiableList(loadedFlowers);
    }

    private static void processFlower(FlowerDefinition flower) {
        if (flower == null) {
            addWarning("Null flower definition found, skipping...");
            return;
        }

        if (!flower.isValid()) {
            addWarning("Invalid flower definition found (missing block_id), skipping...");
            return;
        }

        String blockId = flower.getBlockId();

        if (loadedIds.contains(blockId)) {
            addWarning("Duplicate flower block_id '" + blockId + "' found, skipping...");
            return;
        }

        validateFlower(flower);

        loadedIds.add(blockId);
        loadedFlowers.add(flower);
        AromaAffect.LOGGER.debug(
                "Loaded flower definition: {} (scent: {})", blockId, flower.getScentId());
    }

    private static void validateFlower(FlowerDefinition flower) {
        String blockId = flower.getBlockId();

        if (flower.hasScentId()) {
            String scentId = flower.getScentId();
            if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(scentId)) {
                addWarning(
                        "["
                                + blockId
                                + "] Referenced scent_id '"
                                + scentId
                                + "' does not exist in ScentRegistry");
            }
        } else {
            addWarning(
                    "[" + blockId + "] No scent_id defined, flower will have no associated scent");
        }
    }

    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaAffect.LOGGER.warn(warning);
    }

    public static FlowerDefinition getFlowerById(String blockId) {
        for (FlowerDefinition flower : loadedFlowers) {
            if (flower.getBlockId().equals(blockId)) {
                return flower;
            }
        }
        return null;
    }

    public static List<FlowerDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading flower definitions...");
        return loadAllFlowers();
    }
}
