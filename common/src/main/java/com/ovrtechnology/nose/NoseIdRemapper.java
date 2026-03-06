package com.ovrtechnology.nose;

import java.util.Map;

/**
 * Maps old (legacy) nose item IDs to their current names.
 * Used to preserve items in old worlds after the ID rename.
 */
public final class NoseIdRemapper {

    private static final Map<String, String> OLD_TO_NEW = Map.of(
            "basic_nose", "foragers_nose",
            "gold_nose", "prospectors_nose",
            "diamond_nose", "jewelers_nose",
            "blaze_nose", "dimensional_nose",
            "netherite_nose", "ancient_nose",
            "ultimate_nose", "dragon_nose"
    );

    private NoseIdRemapper() {
    }

    public static Map<String, String> getMappings() {
        return OLD_TO_NEW;
    }

    /**
     * Returns the current ID for a potentially legacy ID.
     * If the ID is not a legacy one, returns it unchanged.
     */
    public static String resolve(String id) {
        return OLD_TO_NEW.getOrDefault(id, id);
    }
}
