package com.ovrtechnology.compat;

import java.util.Set;

/**
 * Structure definitions that only exist in newer Minecraft versions.
 * <p>
 * The client cannot read the world's structure registry, so these definitions are
 * dropped when data loads instead of appearing in menus for a version without them.
 */
public final class StructureCompat {

    //? if >=26.3 {
    private static final Set<String> UNAVAILABLE = Set.of();
    //?} else {
    /*private static final Set<String> UNAVAILABLE = Set.of("minecraft:abandoned_camp");
    *///?}

    private StructureCompat() {}

    public static boolean isAvailable(String structureId) {
        return !UNAVAILABLE.contains(structureId);
    }
}
