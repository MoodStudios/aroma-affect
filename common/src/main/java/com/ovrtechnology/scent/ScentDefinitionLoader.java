package com.ovrtechnology.scent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

public class ScentDefinitionLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String SCENTS_DIR = "aroma_scents";

    @Getter private static List<ScentDefinition> loadedScents = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    public static List<ScentDefinition> loadAllScents() {
        return loadAllScents(ClasspathDataSource.INSTANCE);
    }

    public static List<ScentDefinition> loadAllScents(DataSource dataSource) {
        loadedScents.clear();
        loadedIds.clear();

        Map<ResourceLocation, JsonElement> files = dataSource.listJson(SCENTS_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                ScentDefinition scent = GSON.fromJson(entry.getValue(), ScentDefinition.class);
                processScent(scent);
            } catch (Exception e) {
                AromaAffect.LOGGER.error(
                        "Failed to parse scent {}: {}", entry.getKey(), e.getMessage());
            }
        }

        AromaAffect.LOGGER.info(
                "Loaded {} scent definitions from {} file(s)", loadedScents.size(), files.size());
        return Collections.unmodifiableList(loadedScents);
    }

    private static void processScent(ScentDefinition scent) {
        if (scent == null) {
            AromaAffect.LOGGER.warn("Null scent definition found, skipping...");
            return;
        }

        if (!scent.isValid()) {
            AromaAffect.LOGGER.warn("Invalid scent definition found (missing id), skipping...");
            return;
        }

        String id = scent.getId();

        if (loadedIds.contains(id)) {
            AromaAffect.LOGGER.warn("Duplicate scent ID '{}' found, skipping...", id);
            return;
        }

        validateScent(scent);

        loadedIds.add(id);
        loadedScents.add(scent);
        AromaAffect.LOGGER.debug("Loaded scent definition: {} ({})", id, scent.getFallbackName());
    }

    private static void validateScent(ScentDefinition scent) {
        String id = scent.getId();

        if (scent.getFallbackName() == null || scent.getFallbackName().isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "[{}] No fallback_name defined, will use auto-generated name", id);
        }
    }

    public static boolean hasScentId(String id) {
        return loadedIds.contains(id);
    }

    public static String toJson(ScentDefinition scent) {
        return GSON.toJson(scent);
    }

    public static Gson getGson() {
        return GSON;
    }

    public static List<ScentDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading scent definitions...");
        return loadAllScents();
    }
}
