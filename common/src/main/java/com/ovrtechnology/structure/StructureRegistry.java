package com.ovrtechnology.structure;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.scent.ScentRegistry;
import java.util.*;
import lombok.Getter;

public final class StructureRegistry {

    @Getter
    private static final Map<String, StructureDefinition> structureDefinitions =
            new LinkedHashMap<>();

    @Getter private static boolean initialized = false;

    private StructureRegistry() {
        throw new UnsupportedOperationException("StructureRegistry is a static utility class");
    }

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("StructureRegistry.init() called multiple times!");
            return;
        }

        if (!ScentRegistry.isInitialized()) {
            AromaAffect.LOGGER.warn(
                    "StructureRegistry.init() called before ScentRegistry! Scent validation may fail.");
        }

        AromaAffect.LOGGER.info("Initializing StructureRegistry...");

        List<StructureDefinition> definitions = StructureDefinitionLoader.loadAllStructures();

        for (StructureDefinition definition : definitions) {
            registerStructure(definition);
        }

        initialized = true;
        AromaAffect.LOGGER.info(
                "StructureRegistry initialized with {} structures", structureDefinitions.size());
    }

    private static void registerStructure(StructureDefinition definition) {
        String structureId = definition.getStructureId();

        if (structureDefinitions.containsKey(structureId)) {
            AromaAffect.LOGGER.warn(
                    "Duplicate structure ID in registry: {}, skipping...", structureId);
            return;
        }

        structureDefinitions.put(structureId, definition);
        AromaAffect.LOGGER.debug("Registered structure: {}", structureId);
    }

    public static boolean hasStructure(String structureId) {
        return structureDefinitions.containsKey(structureId);
    }

    public static Iterable<String> getAllStructureIds() {
        return Collections.unmodifiableSet(structureDefinitions.keySet());
    }

    public static Iterable<StructureDefinition> getAllStructures() {
        return Collections.unmodifiableCollection(structureDefinitions.values());
    }

    public static List<StructureDefinition> getAllStructuresAsList() {
        return new ArrayList<>(structureDefinitions.values());
    }

    public static List<StructureDefinition> getVanillaStructures() {
        List<StructureDefinition> result = new ArrayList<>();
        for (StructureDefinition structure : structureDefinitions.values()) {
            if (structure.isVanilla()) {
                result.add(structure);
            }
        }
        return result;
    }

    public static List<StructureDefinition> getModdedStructures() {
        List<StructureDefinition> result = new ArrayList<>();
        for (StructureDefinition structure : structureDefinitions.values()) {
            if (!structure.isVanilla()) {
                result.add(structure);
            }
        }
        return result;
    }

    public static List<String> validateStructureIds(List<String> structureIds) {
        List<String> invalid = new ArrayList<>();
        for (String id : structureIds) {
            if (!hasStructure(id)) {
                invalid.add(id);
            }
        }
        return invalid;
    }

    public static Set<String> getAllNamespaces() {
        Set<String> namespaces = new HashSet<>();
        for (StructureDefinition structure : structureDefinitions.values()) {
            String namespace = structure.getNamespace();
            if (!namespace.isEmpty()) {
                namespaces.add(namespace);
            }
        }
        return namespaces;
    }

    public static void reload() {
        AromaAffect.LOGGER.info("Reloading StructureRegistry...");
        structureDefinitions.clear();

        List<StructureDefinition> definitions = StructureDefinitionLoader.reload();
        for (StructureDefinition definition : definitions) {
            registerStructure(definition);
        }

        AromaAffect.LOGGER.info(
                "StructureRegistry reloaded with {} structures", structureDefinitions.size());
    }

    static void clear() {
        structureDefinitions.clear();
        initialized = false;
    }
}
