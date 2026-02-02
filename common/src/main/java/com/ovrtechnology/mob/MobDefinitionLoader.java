package com.ovrtechnology.mob;

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
 * Loads mob definitions from JSON files.
 */
public class MobDefinitionLoader {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String MOBS_RESOURCE_PATH = "data/aromaaffect/mobs/mobs.json";

    @Getter
    private static List<MobDefinition> loadedMobs = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    @Getter
    private static List<String> validationWarnings = new ArrayList<>();

    public static List<MobDefinition> loadAllMobs() {
        loadedMobs.clear();
        loadedIds.clear();
        validationWarnings.clear();

        try {
            MobDefinition[] mobs = loadMobsFromResource(MOBS_RESOURCE_PATH);
            if (mobs != null) {
                for (MobDefinition mob : mobs) {
                    processMob(mob);
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load mob definitions", e);
        }

        AromaAffect.LOGGER.info("Loaded {} mob definitions", loadedMobs.size());

        if (!validationWarnings.isEmpty()) {
            AromaAffect.LOGGER.warn("Mob loading completed with {} validation warnings", validationWarnings.size());
        }

        return Collections.unmodifiableList(loadedMobs);
    }

    private static void processMob(MobDefinition mob) {
        if (mob == null) {
            addWarning("Null mob definition found, skipping...");
            return;
        }

        if (!mob.isValid()) {
            addWarning("Invalid mob definition found (missing entity_type), skipping...");
            return;
        }

        String entityType = mob.getEntityType();

        if (loadedIds.contains(entityType)) {
            addWarning("Duplicate mob entity_type '" + entityType + "' found, skipping...");
            return;
        }

        validateMob(mob);

        loadedIds.add(entityType);
        loadedMobs.add(mob);
        AromaAffect.LOGGER.debug("Loaded mob definition: {} (scent: {})",
                entityType, mob.getScentId());
    }

    private static void validateMob(MobDefinition mob) {
        String entityType = mob.getEntityType();

        if (mob.hasScentId()) {
            String scentId = mob.getScentId();
            if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(scentId)) {
                addWarning("[" + entityType + "] Referenced scent_id '" + scentId + "' does not exist in ScentRegistry");
            }
        } else {
            addWarning("[" + entityType + "] No scent_id defined, mob will have no associated scent");
        }
    }

    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaAffect.LOGGER.warn(warning);
    }

    private static MobDefinition[] loadMobsFromResource(String resourcePath) {
        try (InputStream inputStream = MobDefinitionLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                AromaAffect.LOGGER.warn("Mob definitions file not found: {}", resourcePath);
                return new MobDefinition[0];
            }

            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);

                if (jsonElement.isJsonArray()) {
                    return GSON.fromJson(jsonElement, MobDefinition[].class);
                } else if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("mobs")) {
                        return GSON.fromJson(jsonObject.get("mobs"), MobDefinition[].class);
                    }
                }

                AromaAffect.LOGGER.warn("Invalid JSON format in: {} (expected array or object with 'mobs' key)", resourcePath);
                return new MobDefinition[0];
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error parsing mob definitions from: {}", resourcePath, e);
            return new MobDefinition[0];
        }
    }

    public static MobDefinition getMobByEntityType(String entityType) {
        for (MobDefinition mob : loadedMobs) {
            if (mob.getEntityType().equals(entityType)) {
                return mob;
            }
        }
        return null;
    }

    public static boolean hasMobEntityType(String entityType) {
        return loadedIds.contains(entityType);
    }

    public static List<MobDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading mob definitions...");
        return loadAllMobs();
    }
}
