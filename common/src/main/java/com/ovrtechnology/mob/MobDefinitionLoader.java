package com.ovrtechnology.mob;

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

public class MobDefinitionLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String MOBS_DIR = "aroma_mobs";

    @Getter private static List<MobDefinition> loadedMobs = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    @Getter private static List<String> validationWarnings = new ArrayList<>();

    public static List<MobDefinition> loadAllMobs() {
        return loadAllMobs(ClasspathDataSource.INSTANCE);
    }

    public static List<MobDefinition> loadAllMobs(DataSource dataSource) {
        loadedMobs.clear();
        loadedIds.clear();
        validationWarnings.clear();

        Map<ResourceLocation, JsonElement> files = dataSource.listJson(MOBS_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                MobDefinition mob = GSON.fromJson(entry.getValue(), MobDefinition.class);
                processMob(mob);
            } catch (Exception e) {
                AromaAffect.LOGGER.error(
                        "Failed to parse mob {}: {}", entry.getKey(), e.getMessage());
            }
        }

        AromaAffect.LOGGER.info(
                "Loaded {} mob definitions from {} file(s)", loadedMobs.size(), files.size());

        if (!validationWarnings.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "Mob loading completed with {} validation warnings", validationWarnings.size());
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
        AromaAffect.LOGGER.debug(
                "Loaded mob definition: {} (scent: {})", entityType, mob.getScentId());
    }

    private static void validateMob(MobDefinition mob) {
        String entityType = mob.getEntityType();

        if (mob.hasScentId()) {
            String scentId = mob.getScentId();
            if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(scentId)) {
                addWarning(
                        "["
                                + entityType
                                + "] Referenced scent_id '"
                                + scentId
                                + "' does not exist in ScentRegistry");
            }
        } else {
            addWarning(
                    "[" + entityType + "] No scent_id defined, mob will have no associated scent");
        }
    }

    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaAffect.LOGGER.warn(warning);
    }

    public static List<MobDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading mob definitions...");
        return loadAllMobs();
    }
}
