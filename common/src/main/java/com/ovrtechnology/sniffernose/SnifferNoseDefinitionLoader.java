package com.ovrtechnology.sniffernose;

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

public class SnifferNoseDefinitionLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String SNIFFER_NOSES_RESOURCE_PATH =
            "data/aromaaffect/noses/sniffer_noses.json";

    private static final String DEFAULT_TEXTURE = "item/sniffer_nose_default";

    @Getter private static List<SnifferNoseDefinition> loadedSnifferNoses = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    public static List<SnifferNoseDefinition> loadAllSnifferNoses() {
        return loadAllSnifferNoses(ClasspathDataSource.INSTANCE);
    }

    public static List<SnifferNoseDefinition> loadAllSnifferNoses(DataSource dataSource) {
        loadedSnifferNoses.clear();
        loadedIds.clear();

        try {
            SnifferNoseDefinition[] snifferNoses =
                    loadSnifferNosesFromResource(dataSource, SNIFFER_NOSES_RESOURCE_PATH);
            if (snifferNoses != null) {
                for (SnifferNoseDefinition snifferNose : snifferNoses) {
                    processSnifferNose(snifferNose);
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load sniffer nose definitions", e);
        }

        AromaAffect.LOGGER.info("Loaded {} sniffer nose definitions", loadedSnifferNoses.size());
        return Collections.unmodifiableList(loadedSnifferNoses);
    }

    public static void reloadInPlace(DataSource dataSource) {
        SnifferNoseDefinition[] newDefs =
                loadSnifferNosesFromResource(dataSource, SNIFFER_NOSES_RESOURCE_PATH);
        int mutated = 0;
        int skipped = 0;
        for (SnifferNoseDefinition src : newDefs) {
            if (src == null || !src.isValid()) continue;
            SnifferNoseDefinition target = findLoadedById(src.getId());
            if (target == null) {
                AromaAffect.LOGGER.warn(
                        "[SnifferNose reload] ID '{}' is not a built-in; custom sniffer noses must use the variant system.",
                        src.getId());
                skipped++;
                continue;
            }
            copyFields(src, target);
            mutated++;
        }
        AromaAffect.LOGGER.info(
                "SnifferNose reload: mutated {} definitions in place, skipped {} unknown IDs",
                mutated,
                skipped);
    }

    private static SnifferNoseDefinition findLoadedById(String id) {
        for (SnifferNoseDefinition d : loadedSnifferNoses) {
            if (id.equals(d.getId())) return d;
        }
        return null;
    }

    private static void copyFields(SnifferNoseDefinition src, SnifferNoseDefinition dst) {
        dst.setImage(src.getImage());
        dst.setModel(src.getModel());
        dst.setTier(src.getTier());
        dst.setDurability(src.getDurability());
        dst.setRepair(src.getRepair());
        dst.setEnabled(src.isEnabled());
    }

    private static void processSnifferNose(SnifferNoseDefinition snifferNose) {
        if (snifferNose == null) {
            AromaAffect.LOGGER.warn("Null sniffer nose definition found, skipping...");
            return;
        }

        if (!snifferNose.isValid()) {
            AromaAffect.LOGGER.warn(
                    "Invalid sniffer nose definition found (missing id), skipping...");
            return;
        }

        String id = snifferNose.getId();

        if (loadedIds.contains(id)) {
            AromaAffect.LOGGER.warn("Duplicate sniffer nose ID '{}' found, skipping...", id);
            return;
        }

        validateAndApplyFallbacks(snifferNose);

        loadedIds.add(id);
        loadedSnifferNoses.add(snifferNose);
        AromaAffect.LOGGER.debug("Loaded sniffer nose definition: {}", id);
    }

    private static void validateAndApplyFallbacks(SnifferNoseDefinition snifferNose) {
        String id = snifferNose.getId();

        String texturePath = snifferNose.getImage();
        if (texturePath == null || texturePath.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "[{}] No texture defined, using fallback: {}", id, DEFAULT_TEXTURE);
            snifferNose.setImage(DEFAULT_TEXTURE);
        } else if (!textureExists(texturePath)) {
            AromaAffect.LOGGER.warn(
                    "[{}] Texture '{}' not found, using fallback: {}",
                    id,
                    texturePath,
                    DEFAULT_TEXTURE);
            snifferNose.setImage(DEFAULT_TEXTURE);
        }

        if (snifferNose.getModel() == null || snifferNose.getModel().isEmpty()) {
            AromaAffect.LOGGER.info(
                    "[{}] No model defined, using default: minecraft:leather_helmet", id);
        }
    }

    private static boolean textureExists(String texturePath) {
        String fullPath = "assets/aromaaffect/textures/" + texturePath + ".png";

        try (InputStream stream =
                SnifferNoseDefinitionLoader.class.getClassLoader().getResourceAsStream(fullPath)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static SnifferNoseDefinition[] loadSnifferNosesFromResource(
            DataSource dataSource, String resourcePath) {
        JsonElement element = dataSource.read(resourcePath);
        if (element == null) {
            AromaAffect.LOGGER.warn("Sniffer noses definitions file not found: {}", resourcePath);
            return new SnifferNoseDefinition[0];
        }
        return JsonResources.parseArrayOrWrapped(
                element, "sniffer_noses", SnifferNoseDefinition[].class, GSON, resourcePath);
    }

    public static String toJson(SnifferNoseDefinition snifferNose) {
        return GSON.toJson(snifferNose);
    }

    public static Gson getGson() {
        return GSON;
    }
}
