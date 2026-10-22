package com.ovrtechnology.ability;

import com.ovrtechnology.AromaAffect;

import java.util.*;

/**
 * Central registry for all abilities in Aroma Affect.
 * 
 * <p>
 * This registry provides a decoupled way to manage abilities without
 * hardcoding references to specific ability implementations. Abilities
 * should be registered during mod initialization.
 * </p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // During mod init
 * AbilityRegistry.register(PreciseSnifferAbility.INSTANCE);
 * 
 * // When handling events
 * for (BlockInteractionAbility ability : AbilityRegistry.getBlockInteractionAbilities()) {
 *     if (ability.isValidTarget(block) && ability.canUse(player)) {
 *         ability.onInteract(player, pos);
 *     }
 * }
 * }</pre>
 * 
 * @see Ability
 * @see BlockInteractionAbility
 */
public final class AbilityRegistry {

    /**
     * Map of all registered abilities by their ID.
     */
    private static final Map<String, Ability> ABILITIES = new HashMap<>();

    /**
     * Cached list of block interaction abilities for performance.
     */
    private static final List<BlockInteractionAbility> BLOCK_INTERACTION_CACHE = new ArrayList<>();

    /**
     * Whether the registry has been initialized.
     */
    private static boolean initialized = false;

    private AbilityRegistry() {}

    /**
     * Registers an ability with the registry.
     * 
     * <p>
     * If an ability with the same ID is already registered, it will be
     * replaced and a warning will be logged.
     * </p>
     * 
     * @param ability the ability to register
     * @throws NullPointerException if ability or ability.getId() is null
     */
    public static void register(Ability ability) {
        Objects.requireNonNull(ability, "Ability cannot be null");
        Objects.requireNonNull(ability.getId(), "Ability ID cannot be null");

        String id = ability.getId();

        if (ABILITIES.containsKey(id)) {
            AromaAffect.LOGGER.warn("Replacing existing ability with ID: {}", id);
        }

        ABILITIES.put(id, ability);

        // Update caches
        if (ability instanceof BlockInteractionAbility blockAbility) {
            // Remove any existing ability with same ID from cache
            BLOCK_INTERACTION_CACHE.removeIf(a -> a.getId().equals(id));
            BLOCK_INTERACTION_CACHE.add(blockAbility);
        }

        AromaAffect.LOGGER.debug("Registered ability: {} ({})", id, ability.getClass().getSimpleName());
    }

    /**
     * Gets an ability by its ID.
     * 
     * @param id the ability ID
     * @return an Optional containing the ability if found
     */
    public static Optional<Ability> get(String id) {
        return Optional.ofNullable(ABILITIES.get(id));
    }

    /**
     * Gets an ability by its ID, cast to the specified type.
     * 
     * @param id   the ability ID
     * @param type the expected ability type
     * @param <T>  the ability type
     * @return an Optional containing the ability if found and matches type
     */
    public static <T extends Ability> Optional<T> get(String id, Class<T> type) {
        Ability ability = ABILITIES.get(id);
        if (ability != null && type.isInstance(ability)) {
            return Optional.of(type.cast(ability));
        }
        return Optional.empty();
    }

    /**
     * Gets all registered abilities of a specific type.
     * 
     * @param type the ability type to filter by
     * @param <T>  the ability type
     * @return an unmodifiable list of abilities of the specified type
     */
    public static <T extends Ability> List<T> getByType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Ability ability : ABILITIES.values()) {
            if (type.isInstance(ability)) {
                result.add(type.cast(ability));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Gets all registered block interaction abilities.
     * 
     * <p>
     * This method returns a cached list for performance, as block interaction
     * abilities are frequently queried during gameplay.
     * </p>
     * 
     * @return an unmodifiable list of block interaction abilities
     */
    public static List<BlockInteractionAbility> getBlockInteractionAbilities() {
        return Collections.unmodifiableList(BLOCK_INTERACTION_CACHE);
    }

    /**
     * Gets all registered abilities.
     * 
     * @return an unmodifiable collection of all abilities
     */
    public static Collection<Ability> getAll() {
        return Collections.unmodifiableCollection(ABILITIES.values());
    }

    /**
     * Gets all registered ability IDs.
     * 
     * @return an unmodifiable set of ability IDs
     */
    public static Set<String> getAllIds() {
        return Collections.unmodifiableSet(ABILITIES.keySet());
    }

    /**
     * Checks if an ability with the given ID is registered.
     * 
     * @param id the ability ID
     * @return true if an ability with this ID exists
     */
    public static boolean contains(String id) {
        return ABILITIES.containsKey(id);
    }

    /**
     * Gets the number of registered abilities.
     * 
     * @return the count of registered abilities
     */
    public static int size() {
        return ABILITIES.size();
    }

    /**
     * Initializes the ability registry.
     * Should be called during mod initialization after all abilities are registered.
     */
    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("AbilityRegistry.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("AbilityRegistry initialized with {} abilities ({} block interaction)",
                ABILITIES.size(), BLOCK_INTERACTION_CACHE.size());

        initialized = true;
    }

    /**
     * Clears all registered abilities.
     * 
     * <p>
     * This should only be used for testing or mod reloading scenarios.
     * </p>
     */
    public static void clear() {
        ABILITIES.clear();
        BLOCK_INTERACTION_CACHE.clear();
        initialized = false;
        AromaAffect.LOGGER.info("AbilityRegistry cleared");
    }
}
