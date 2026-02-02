package com.ovrtechnology.flower;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentRegistry;
import lombok.Getter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads flower definitions from JSON files.
 */
public class FlowerDefinitionLoader {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String FLOWERS_RESOURCE_PATH = "data/aromaaffect/flowers/flowers.json";

    @Getter
    private static List<FlowerDefinition> loadedFlowers = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    @Getter
    private static List<String> validationWarnings = new ArrayList<>();

    public static List<FlowerDefinition> loadAllFlowers() {
        loadedFlowers.clear();
        loadedIds.clear();
        validationWarnings.clear();

        try {
            FlowerDefinition[] flowers = loadFlowersFromResource(FLOWERS_RESOURCE_PATH);
            if (flowers != null) {
                for (FlowerDefinition flower : flowers) {
                    processFlower(flower);
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load flower definitions", e);
        }

        AromaAffect.LOGGER.info("Loaded {} flower definitions", loadedFlowers.size());

        if (!validationWarnings.isEmpty()) {
            AromaAffect.LOGGER.warn("Flower loading completed with {} validation warnings", validationWarnings.size());
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
        AromaAffect.LOGGER.debug("Loaded flower definition: {} (scent: {})",
                blockId, flower.getScentId());
    }

    private static void validateFlower(FlowerDefinition flower) {
        String blockId = flower.getBlockId();

        if (flower.hasScentId()) {
            String scentId = flower.getScentId();
            if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(scentId)) {
                addWarning("[" + blockId + "] Referenced scent_id '" + scentId + "' does not exist in ScentRegistry");
            }
        } else {
            addWarning("[" + blockId + "] No scent_id defined, flower will have no associated scent");
        }
    }

    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaAffect.LOGGER.warn(warning);
    }

    private static FlowerDefinition[] loadFlowersFromResource(String resourcePath) {
        try (InputStream inputStream = FlowerDefinitionLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                AromaAffect.LOGGER.warn("Flower definitions file not found: {}", resourcePath);
                return new FlowerDefinition[0];
            }

            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);

                if (jsonElement.isJsonArray()) {
                    return GSON.fromJson(jsonElement, FlowerDefinition[].class);
                } else if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("flowers")) {
                        return GSON.fromJson(jsonObject.get("flowers"), FlowerDefinition[].class);
                    }
                }

                AromaAffect.LOGGER.warn("Invalid JSON format in: {} (expected array or object with 'flowers' key)", resourcePath);
                return new FlowerDefinition[0];
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error parsing flower definitions from: {}", resourcePath, e);
            return new FlowerDefinition[0];
        }
    }

    public static FlowerDefinition getFlowerById(String blockId) {
        for (FlowerDefinition flower : loadedFlowers) {
            if (flower.getBlockId().equals(blockId)) {
                return flower;
            }
        }
        return null;
    }

    public static boolean hasFlowerId(String blockId) {
        return loadedIds.contains(blockId);
    }

    public static List<FlowerDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading flower definitions...");
        return loadAllFlowers();
    }
}
