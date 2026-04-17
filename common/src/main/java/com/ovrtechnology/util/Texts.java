package com.ovrtechnology.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Thin wrapper around {@link Component} construction so that Text / Component
 * API drift between Minecraft versions (factory renames, return-type changes,
 * new builder shapes) is absorbed in this file instead of every caller.
 * <p>
 * Keep this class free of logic — it is a pure indirection layer.
 */
public final class Texts {

    private Texts() {}

    /** Translatable component; looks up {@code key} in the language file. */
    public static MutableComponent tr(String key) {
        return Component.translatable(key);
    }

    /** Translatable component with formatting args. */
    public static MutableComponent tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /** Literal (already-rendered) text component. */
    public static MutableComponent lit(String text) {
        return Component.literal(text);
    }

    /** Empty component. */
    public static MutableComponent empty() {
        return Component.empty();
    }
}
