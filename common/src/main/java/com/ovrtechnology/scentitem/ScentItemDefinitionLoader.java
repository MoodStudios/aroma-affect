package com.ovrtechnology.scentitem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.data.JsonResources;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

public class ScentItemDefinitionLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String SCENT_ITEMS_RESOURCE_PATH =
            "data/aromaaffect/scents/scent_items.json";

    private static final String DEFAULT_TEXTURE = "item/scent_default";

    @Getter private static List<ScentItemDefinition> loadedScentItems = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    public static List<ScentItemDefinition> loadAllScentItems() {
        return loadAllScentItems(ClasspathDataSource.INSTANCE);
    }

    public static List<ScentItemDefinition> loadAllScentItems(DataSource dataSource) {
        loadedScentItems.clear();
        loadedIds.clear();

        try {
            ScentItemDefinition[] scentItems =
                    loadScentItemsFromResource(dataSource, SCENT_ITEMS_RESOURCE_PATH);
            if (scentItems != null) {
                for (ScentItemDefinition scentItem : scentItems) {
                    processScentItem(scentItem);
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load scent item definitions", e);
        }

        AromaAffect.LOGGER.info("Loaded {} scent item definitions", loadedScentItems.size());
        return Collections.unmodifiableList(loadedScentItems);
    }

    public static void reloadInPlace(DataSource dataSource) {
        ScentItemDefinition[] newDefs =
                loadScentItemsFromResource(dataSource, SCENT_ITEMS_RESOURCE_PATH);
        int mutated = 0;
        int skipped = 0;
        for (ScentItemDefinition src : newDefs) {
            if (src == null || !src.isValid()) continue;
            ScentItemDefinition target = findLoadedById(src.getId());
            if (target == null) {
                AromaAffect.LOGGER.warn(
                        "[ScentItem reload] ID '{}' is not a built-in; custom scent items must use the variant system.",
                        src.getId());
                skipped++;
                continue;
            }
            copyFields(src, target);
            mutated++;
        }
        AromaAffect.LOGGER.info(
                "ScentItem reload: mutated {} definitions in place, skipped {} unknown IDs",
                mutated,
                skipped);
    }

    private static ScentItemDefinition findLoadedById(String id) {
        for (ScentItemDefinition d : loadedScentItems) {
            if (id.equals(d.getId())) return d;
        }
        return null;
    }

    private static void copyFields(ScentItemDefinition src, ScentItemDefinition dst) {
        dst.setImage(src.getImage());
        dst.setModel(src.getModel());
        dst.setType(src.getType());
        dst.setScent(src.getScent());
        dst.setFallbackName(src.getFallbackName());
        dst.setDescription(src.getDescription());
        dst.setPriority(src.getPriority());
        dst.setEnabled(src.isEnabled());
    }

    private static void processScentItem(ScentItemDefinition scentItem) {
        if (scentItem == null) {
            AromaAffect.LOGGER.warn("Null scent item definition found, skipping...");
            return;
        }

        if (!scentItem.isValid()) {
            AromaAffect.LOGGER.warn(
                    "Invalid scent item definition found (missing id), skipping...");
            return;
        }

        String id = scentItem.getId();

        if (loadedIds.contains(id)) {
            AromaAffect.LOGGER.warn("Duplicate scent item ID '{}' found, skipping...", id);
            return;
        }

        validateAndApplyFallbacks(scentItem);

        loadedIds.add(id);
        loadedScentItems.add(scentItem);
        AromaAffect.LOGGER.debug(
                "Loaded scent item definition: {} ({})", id, scentItem.getFallbackName());
    }

    private static void validateAndApplyFallbacks(ScentItemDefinition scentItem) {
        String id = scentItem.getId();

        String texturePath = scentItem.getImage();
        if (texturePath == null || texturePath.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "[{}] No texture defined, using fallback: {}", id, DEFAULT_TEXTURE);
            scentItem.setImage(DEFAULT_TEXTURE);
        } else if (!textureExists(texturePath)) {
            AromaAffect.LOGGER.warn(
                    "[{}] Texture '{}' not found, using fallback: {}",
                    id,
                    texturePath,
                    DEFAULT_TEXTURE);
            scentItem.setImage(DEFAULT_TEXTURE);
        }

        if (scentItem.getModel() == null || scentItem.getModel().isEmpty()) {
            AromaAffect.LOGGER.info("[{}] No model defined, using default: minecraft:paper", id);
        }

        int priority = scentItem.getPriority();
        if (priority < 1 || priority > 10) {
            AromaAffect.LOGGER.warn(
                    "[{}] Priority {} out of bounds (1-10), will be clamped", id, priority);
        }
    }

    private static boolean textureExists(String texturePath) {
        String fullPath = "assets/aromaaffect/textures/" + texturePath + ".png";

        try (InputStream stream =
                ScentItemDefinitionLoader.class.getClassLoader().getResourceAsStream(fullPath)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static ScentItemDefinition[] loadScentItemsFromResource(
            DataSource dataSource, String resourcePath) {
        JsonElement element = dataSource.read(resourcePath);
        if (element == null) {
            AromaAffect.LOGGER.warn("Scent items definitions file not found: {}", resourcePath);
            return new ScentItemDefinition[0];
        }
        return JsonResources.parseArrayOrWrapped(
                element, "scents", ScentItemDefinition[].class, GSON, resourcePath);
    }

    public static String toJson(ScentItemDefinition scentItem) {
        return GSON.toJson(scentItem);
    }

    public static Gson getGson() {
        return GSON;
    }
}
