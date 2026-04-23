package com.ovrtechnology.scent;

import com.ovrtechnology.AromaAffect;
import java.util.*;
import lombok.Getter;

public final class ScentRegistry {

    @Getter
    private static final Map<String, ScentDefinition> scentDefinitions = new LinkedHashMap<>();

    @Getter private static boolean initialized = false;

    private ScentRegistry() {
        throw new UnsupportedOperationException("ScentRegistry is a static utility class");
    }

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("ScentRegistry.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing ScentRegistry...");

        List<ScentDefinition> definitions = ScentDefinitionLoader.loadAllScents();

        for (ScentDefinition definition : definitions) {
            registerScent(definition);
        }

        initialized = true;
        AromaAffect.LOGGER.info(
                "ScentRegistry initialized with {} scents", scentDefinitions.size());
    }

    private static void registerScent(ScentDefinition definition) {
        String id = definition.getId();

        if (scentDefinitions.containsKey(id)) {
            AromaAffect.LOGGER.warn("Duplicate scent ID in registry: {}, skipping...", id);
            return;
        }

        scentDefinitions.put(id, definition);
        AromaAffect.LOGGER.debug("Registered scent: {}", id);
    }

    public static Optional<ScentDefinition> getScent(String id) {
        return Optional.ofNullable(scentDefinitions.get(id));
    }

    public static Iterable<String> getAllScentIds() {
        return Collections.unmodifiableSet(scentDefinitions.keySet());
    }

    public static Optional<ScentDefinition> getScentByName(String name) {
        if (name == null) return Optional.empty();
        for (ScentDefinition def : scentDefinitions.values()) {
            if (name.equalsIgnoreCase(def.getFallbackName())) {
                return Optional.of(def);
            }
        }
        return Optional.empty();
    }

    public static boolean hasScent(String id) {
        return scentDefinitions.containsKey(id);
    }

    public static String getDisplayName(String id) {
        ScentDefinition scent = scentDefinitions.get(id);
        if (scent == null) {
            return "Unknown Scent";
        }

        return scent.getFallbackName();
    }

    public static void reload() {
        AromaAffect.LOGGER.info("Reloading ScentRegistry...");
        scentDefinitions.clear();

        List<ScentDefinition> definitions = ScentDefinitionLoader.reload();
        for (ScentDefinition definition : definitions) {
            registerScent(definition);
        }

        AromaAffect.LOGGER.info("ScentRegistry reloaded with {} scents", scentDefinitions.size());
    }

    static void clear() {
        scentDefinitions.clear();
        initialized = false;
    }
}
