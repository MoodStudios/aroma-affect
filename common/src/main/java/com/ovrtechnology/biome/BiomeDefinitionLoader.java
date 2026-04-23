package com.ovrtechnology.biome;

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

public class BiomeDefinitionLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String BIOMES_DIR = "aroma_biomes";

    @Getter private static List<BiomeDefinition> loadedBiomes = new ArrayList<>();

    private static Set<String> loadedIds = new HashSet<>();

    @Getter private static List<String> validationWarnings = new ArrayList<>();

    public static List<BiomeDefinition> loadAllBiomes() {
        return loadAllBiomes(ClasspathDataSource.INSTANCE);
    }

    public static List<BiomeDefinition> loadAllBiomes(DataSource dataSource) {
        loadedBiomes.clear();
        loadedIds.clear();
        validationWarnings.clear();

        Map<ResourceLocation, JsonElement> files = dataSource.listJson(BIOMES_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                BiomeDefinition biome = GSON.fromJson(entry.getValue(), BiomeDefinition.class);
                processBiome(biome);
            } catch (Exception e) {
                AromaAffect.LOGGER.error(
                        "Failed to parse biome {}: {}", entry.getKey(), e.getMessage());
            }
        }

        AromaAffect.LOGGER.info(
                "Loaded {} biome definitions from {} file(s)", loadedBiomes.size(), files.size());

        if (!validationWarnings.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "Biome loading completed with {} validation warnings",
                    validationWarnings.size());
        }

        return Collections.unmodifiableList(loadedBiomes);
    }

    private static void processBiome(BiomeDefinition biome) {
        if (biome == null) {
            addWarning("Null biome definition found, skipping...");
            return;
        }

        if (!biome.isValid()) {
            addWarning("Invalid biome definition found (missing biome_id), skipping...");
            return;
        }

        String biomeId = biome.getBiomeId();

        if (loadedIds.contains(biomeId)) {
            addWarning("Duplicate biome_id '" + biomeId + "' found, skipping...");
            return;
        }

        validateBiome(biome);

        loadedIds.add(biomeId);
        loadedBiomes.add(biome);
        AromaAffect.LOGGER.debug(
                "Loaded biome definition: {} (color: {}, scent: {})",
                biomeId,
                biome.getColorHtml(),
                biome.getScentId());
    }

    private static void validateBiome(BiomeDefinition biome) {
        String biomeId = biome.getBiomeId();

        if (!biome.hasValidBiomeIdFormat()) {
            addWarning(
                    "["
                            + biomeId
                            + "] Invalid biome_id format - should be 'namespace:path' (e.g., 'minecraft:jungle')");
        }

        String rawColor = biome.getRawColorHtml();
        if (rawColor == null || rawColor.isEmpty()) {
            addWarning(
                    "["
                            + biomeId
                            + "] No color_html defined, using default: "
                            + BiomeDefinition.DEFAULT_COLOR);
        } else if (!BiomeDefinition.isValidHtmlColor(rawColor)) {
            addWarning(
                    "["
                            + biomeId
                            + "] Invalid color_html format '"
                            + rawColor
                            + "', using default: "
                            + BiomeDefinition.DEFAULT_COLOR);
        }

        if (biome.hasScentId()) {
            String scentId = biome.getScentId();
            if (ScentRegistry.isInitialized() && !ScentRegistry.hasScent(scentId)) {
                addWarning(
                        "["
                                + biomeId
                                + "] Referenced scent_id '"
                                + scentId
                                + "' does not exist in ScentRegistry");
            }
        } else {
            addWarning(
                    "[" + biomeId + "] No scent_id defined, biome will have no associated scent");
        }

        String rawImage = biome.getRawImage();
        if (rawImage == null || rawImage.isEmpty()) {
            AromaAffect.LOGGER.debug(
                    "[{}] No image defined, using default: {}",
                    biomeId,
                    BiomeDefinition.DEFAULT_IMAGE);
        }
    }

    private static void addWarning(String warning) {
        validationWarnings.add(warning);
        AromaAffect.LOGGER.warn(warning);
    }

    public static BiomeDefinition getBiomeById(String biomeId) {
        for (BiomeDefinition biome : loadedBiomes) {
            if (biome.getBiomeId().equals(biomeId)) {
                return biome;
            }
        }
        return null;
    }

    public static boolean hasBiomeId(String biomeId) {
        return loadedIds.contains(biomeId);
    }

    public static List<BiomeDefinition> getVanillaBiomes() {
        List<BiomeDefinition> result = new ArrayList<>();
        for (BiomeDefinition biome : loadedBiomes) {
            if (biome.isVanilla()) {
                result.add(biome);
            }
        }
        return result;
    }

    public static List<BiomeDefinition> getModdedBiomes() {
        List<BiomeDefinition> result = new ArrayList<>();
        for (BiomeDefinition biome : loadedBiomes) {
            if (!biome.isVanilla()) {
                result.add(biome);
            }
        }
        return result;
    }

    public static String toJson(BiomeDefinition biome) {
        return GSON.toJson(biome);
    }

    public static Gson getGson() {
        return GSON;
    }

    public static List<BiomeDefinition> reload() {
        AromaAffect.LOGGER.info("Reloading biome definitions...");
        return loadAllBiomes();
    }

    public static boolean hasValidationWarnings() {
        return !validationWarnings.isEmpty();
    }
}
