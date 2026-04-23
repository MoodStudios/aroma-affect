package com.ovrtechnology.ability;

import com.ovrtechnology.AromaAffect;
import java.util.*;

public final class AbilityRegistry {

    private static final Map<String, Ability> ABILITIES = new HashMap<>();

    private static final List<BlockInteractionAbility> BLOCK_INTERACTION_CACHE = new ArrayList<>();

    private static boolean initialized = false;

    private AbilityRegistry() {}

    public static void register(Ability ability) {
        Objects.requireNonNull(ability, "Ability cannot be null");
        Objects.requireNonNull(ability.getId(), "Ability ID cannot be null");

        String id = ability.getId();

        if (ABILITIES.containsKey(id)) {
            AromaAffect.LOGGER.warn("Replacing existing ability with ID: {}", id);
        }

        ABILITIES.put(id, ability);

        if (ability instanceof BlockInteractionAbility blockAbility) {

            BLOCK_INTERACTION_CACHE.removeIf(a -> a.getId().equals(id));
            BLOCK_INTERACTION_CACHE.add(blockAbility);
        }

        AromaAffect.LOGGER.debug(
                "Registered ability: {} ({})", id, ability.getClass().getSimpleName());
    }

    public static Optional<Ability> get(String id) {
        return Optional.ofNullable(ABILITIES.get(id));
    }

    public static <T extends Ability> Optional<T> get(String id, Class<T> type) {
        Ability ability = ABILITIES.get(id);
        if (ability != null && type.isInstance(ability)) {
            return Optional.of(type.cast(ability));
        }
        return Optional.empty();
    }

    public static List<BlockInteractionAbility> getBlockInteractionAbilities() {
        return Collections.unmodifiableList(BLOCK_INTERACTION_CACHE);
    }

    public static boolean contains(String id) {
        return ABILITIES.containsKey(id);
    }

    public static int size() {
        return ABILITIES.size();
    }

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("AbilityRegistry.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info(
                "AbilityRegistry initialized with {} abilities ({} block interaction)",
                ABILITIES.size(),
                BLOCK_INTERACTION_CACHE.size());

        initialized = true;
    }

    public static void clear() {
        ABILITIES.clear();
        BLOCK_INTERACTION_CACHE.clear();
        initialized = false;
        AromaAffect.LOGGER.info("AbilityRegistry cleared");
    }
}
