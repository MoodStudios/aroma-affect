package com.ovrtechnology.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.data.JsonResources;
import java.util.*;
import lombok.Getter;

public class AbilityDefinitionLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String ABILITIES_RESOURCE_PATH =
            "data/aromaaffect/abilities/abilities.json";

    @Getter private static Map<String, AbilityDefinition> loadedAbilities = new HashMap<>();

    private static boolean initialized = false;

    public static Map<String, AbilityDefinition> loadAllAbilities() {
        return loadAllAbilities(ClasspathDataSource.INSTANCE);
    }

    public static Map<String, AbilityDefinition> loadAllAbilities(DataSource dataSource) {
        loadedAbilities.clear();

        try {
            AbilityDefinition[] abilities =
                    loadAbilitiesFromResource(dataSource, ABILITIES_RESOURCE_PATH);
            if (abilities != null) {
                for (AbilityDefinition ability : abilities) {
                    if (ability != null && ability.isValid()) {
                        loadedAbilities.put(ability.getId(), ability);
                        AromaAffect.LOGGER.info(
                                "Loaded ability definition: {} (type: {})",
                                ability.getId(),
                                ability.getType());
                    } else {
                        AromaAffect.LOGGER.warn("Invalid ability definition found, skipping...");
                    }
                }
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to load ability definitions", e);
        }

        AromaAffect.LOGGER.info("Loaded {} ability definitions", loadedAbilities.size());
        initialized = true;
        return Collections.unmodifiableMap(loadedAbilities);
    }

    private static AbilityDefinition[] loadAbilitiesFromResource(
            DataSource dataSource, String resourcePath) {
        JsonElement element = dataSource.read(resourcePath);
        if (element == null) {
            AromaAffect.LOGGER.warn("Ability definitions file not found: {}", resourcePath);
            return new AbilityDefinition[0];
        }
        return JsonResources.parseArrayOrWrapped(
                element, "abilities", AbilityDefinition[].class, GSON, resourcePath);
    }

    public static AbilityDefinition getAbilityById(String id) {
        return loadedAbilities.get(id);
    }

    public static Optional<AbilityDefinition> getAbility(String id) {
        return Optional.ofNullable(loadedAbilities.get(id));
    }

    public static List<String> validateAbilityIds(List<String> abilityIds) {
        List<String> invalid = new ArrayList<>();
        for (String id : abilityIds) {
            if (!loadedAbilities.containsKey(id)) {
                invalid.add(id);
            }
        }
        return invalid;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static Gson getGson() {
        return GSON;
    }
}
