package com.ovrtechnology.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.ClasspathDataSource;
import com.ovrtechnology.data.DataSource;
import com.ovrtechnology.data.JsonResources;
import lombok.Getter;

import java.util.*;

/**
 * Loads ability definitions from JSON files.
 * 
 * <p>
 * Parses the data/aromaaffect/abilities/abilities.json file to load
 * all ability definitions. This provides a central registry of all
 * abilities available in the mod, separate from nose definitions.
 * </p>
 * 
 * <h2>File Structure:</h2>
 * <pre>{@code
 * {
 *   "abilities": [
 *     {
 *       "id": "precise_sniffer",
 *       "type": "block_interaction",
 *       "description": "...",
 *       "config": { ... }
 *     }
 *   ]
 * }
 * }</pre>
 * 
 * @see AbilityDefinition
 * @see AbilityRegistry
 */
public class AbilityDefinitionLoader {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Resource path for the abilities definition file.
     */
    private static final String ABILITIES_RESOURCE_PATH = "data/aromaaffect/abilities/abilities.json";

    /**
     * Cached map of loaded ability definitions by ID.
     */
    @Getter
    private static Map<String, AbilityDefinition> loadedAbilities = new HashMap<>();

    /**
     * Whether the loader has been initialized.
     */
    private static boolean initialized = false;

    /**
     * Load all ability definitions from the abilities.json file.
     * 
     * @return unmodifiable map of ability ID to definition
     */
    public static Map<String, AbilityDefinition> loadAllAbilities() {
        return loadAllAbilities(ClasspathDataSource.INSTANCE);
    }

    public static Map<String, AbilityDefinition> loadAllAbilities(DataSource dataSource) {
        loadedAbilities.clear();

        try {
            AbilityDefinition[] abilities = loadAbilitiesFromResource(dataSource, ABILITIES_RESOURCE_PATH);
            if (abilities != null) {
                for (AbilityDefinition ability : abilities) {
                    if (ability != null && ability.isValid()) {
                        loadedAbilities.put(ability.getId(), ability);
                        AromaAffect.LOGGER.info("Loaded ability definition: {} (type: {})",
                                ability.getId(), ability.getType());
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

    private static AbilityDefinition[] loadAbilitiesFromResource(DataSource dataSource, String resourcePath) {
        JsonElement element = dataSource.read(resourcePath);
        if (element == null) {
            AromaAffect.LOGGER.warn("Ability definitions file not found: {}", resourcePath);
            return new AbilityDefinition[0];
        }
        return JsonResources.parseArrayOrWrapped(element, "abilities", AbilityDefinition[].class, GSON, resourcePath);
    }

    /**
     * Get an ability definition by ID.
     * 
     * @param id the ability ID
     * @return the definition or null if not found
     */
    public static AbilityDefinition getAbilityById(String id) {
        return loadedAbilities.get(id);
    }

    /**
     * Get an ability definition by ID, wrapped in Optional.
     * 
     * @param id the ability ID
     * @return Optional containing the definition if found
     */
    public static Optional<AbilityDefinition> getAbility(String id) {
        return Optional.ofNullable(loadedAbilities.get(id));
    }

    /**
     * Check if an ability ID is defined.
     * 
     * @param id the ability ID
     * @return true if the ability is defined
     */
    public static boolean isAbilityDefined(String id) {
        return loadedAbilities.containsKey(id);
    }

    /**
     * Get all ability IDs.
     * 
     * @return unmodifiable set of all ability IDs
     */
    public static Set<String> getAllAbilityIds() {
        return Collections.unmodifiableSet(loadedAbilities.keySet());
    }

    /**
     * Get all abilities of a specific type.
     * 
     * @param type the ability type (e.g., "passive", "block_interaction")
     * @return list of abilities matching the type
     */
    public static List<AbilityDefinition> getAbilitiesByType(String type) {
        List<AbilityDefinition> result = new ArrayList<>();
        for (AbilityDefinition ability : loadedAbilities.values()) {
            if (type.equals(ability.getType())) {
                result.add(ability);
            }
        }
        return result;
    }

    /**
     * Validate a list of ability IDs and return invalid ones.
     * 
     * @param abilityIds list of ability IDs to validate
     * @return list of invalid ability IDs (not defined in abilities.json)
     */
    public static List<String> validateAbilityIds(List<String> abilityIds) {
        List<String> invalid = new ArrayList<>();
        for (String id : abilityIds) {
            if (!loadedAbilities.containsKey(id)) {
                invalid.add(id);
            }
        }
        return invalid;
    }

    /**
     * Check if the loader has been initialized.
     * 
     * @return true if loadAllAbilities() has been called
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Get the GSON instance for external use.
     * 
     * @return the Gson instance
     */
    public static Gson getGson() {
        return GSON;
    }
}
