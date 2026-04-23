package com.ovrtechnology.nose;

import java.util.Map;

public final class NoseIdRemapper {

    private static final Map<String, String> OLD_TO_NEW =
            Map.of(
                    "basic_nose", "foragers_nose",
                    "gold_nose", "prospectors_nose",
                    "diamond_nose", "jewelers_nose",
                    "blaze_nose", "dimensional_nose",
                    "netherite_nose", "ancient_nose",
                    "ultimate_nose", "dragon_nose");

    private NoseIdRemapper() {}

    public static Map<String, String> getMappings() {
        return OLD_TO_NEW;
    }

    public static String resolve(String id) {
        return OLD_TO_NEW.getOrDefault(id, id);
    }
}
