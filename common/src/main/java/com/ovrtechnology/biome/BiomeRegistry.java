package com.ovrtechnology.biome;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentRegistry;
import java.util.*;
import lombok.Getter;

public final class BiomeRegistry {

    @Getter
    private static final Map<String, BiomeDefinition> biomeDefinitions = new LinkedHashMap<>();

    @Getter private static boolean initialized = false;

    private BiomeRegistry() {
        throw new UnsupportedOperationException("BiomeRegistry is a static utility class");
    }

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("BiomeRegistry.init() called multiple times!");
            return;
        }

        if (!ScentRegistry.isInitialized()) {
            AromaAffect.LOGGER.warn(
                    "BiomeRegistry.init() called before ScentRegistry! Scent validation may fail.");
        }

        AromaAffect.LOGGER.info("Initializing BiomeRegistry...");

        List<BiomeDefinition> definitions = BiomeDefinitionLoader.loadAllBiomes();

        for (BiomeDefinition definition : definitions) {
            registerBiome(definition);
        }

        initialized = true;
        AromaAffect.LOGGER.info(
                "BiomeRegistry initialized with {} biomes", biomeDefinitions.size());
    }

    private static void registerBiome(BiomeDefinition definition) {
        String biomeId = definition.getBiomeId();

        if (biomeDefinitions.containsKey(biomeId)) {
            AromaAffect.LOGGER.warn("Duplicate biome ID in registry: {}, skipping...", biomeId);
            return;
        }

        biomeDefinitions.put(biomeId, definition);
        AromaAffect.LOGGER.debug("Registered biome: {}", biomeId);
    }

    public static Optional<BiomeDefinition> getBiome(String biomeId) {
        return Optional.ofNullable(biomeDefinitions.get(biomeId));
    }

    public static boolean hasBiome(String biomeId) {
        return biomeDefinitions.containsKey(biomeId);
    }

    public static Iterable<String> getAllBiomeIds() {
        return Collections.unmodifiableSet(biomeDefinitions.keySet());
    }

    public static Iterable<BiomeDefinition> getAllBiomes() {
        return Collections.unmodifiableCollection(biomeDefinitions.values());
    }

    public static List<BiomeDefinition> getVanillaBiomes() {
        List<BiomeDefinition> result = new ArrayList<>();
        for (BiomeDefinition biome : biomeDefinitions.values()) {
            if (biome.isVanilla()) {
                result.add(biome);
            }
        }
        return result;
    }

    public static List<BiomeDefinition> getModdedBiomes() {
        List<BiomeDefinition> result = new ArrayList<>();
        for (BiomeDefinition biome : biomeDefinitions.values()) {
            if (!biome.isVanilla()) {
                result.add(biome);
            }
        }
        return result;
    }

    public static List<String> validateBiomeIds(List<String> biomeIds) {
        List<String> invalid = new ArrayList<>();
        for (String id : biomeIds) {
            if (!hasBiome(id)) {
                invalid.add(id);
            }
        }
        return invalid;
    }

    public static Set<String> getAllNamespaces() {
        Set<String> namespaces = new HashSet<>();
        for (BiomeDefinition biome : biomeDefinitions.values()) {
            String namespace = biome.getNamespace();
            if (!namespace.isEmpty()) {
                namespaces.add(namespace);
            }
        }
        return namespaces;
    }

    public static void reload() {
        AromaAffect.LOGGER.info("Reloading BiomeRegistry...");
        biomeDefinitions.clear();

        List<BiomeDefinition> definitions = BiomeDefinitionLoader.reload();
        for (BiomeDefinition definition : definitions) {
            registerBiome(definition);
        }

        AromaAffect.LOGGER.info("BiomeRegistry reloaded with {} biomes", biomeDefinitions.size());
    }

    static void clear() {
        biomeDefinitions.clear();
        initialized = false;
    }
}
