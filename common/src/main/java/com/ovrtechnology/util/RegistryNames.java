package com.ovrtechnology.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Resolves player-facing names for registry objects, preferring the game's own
 * translations over names derived from the resource id.
 *
 * <p>Vanilla ships translated names for blocks, entities and biomes in every
 * language it supports, so routing through the registry gives those categories
 * localization for free. Structures have no vanilla translation keys, so they
 * take an explicit fallback supplied by the caller.</p>
 */
public final class RegistryNames {

    private RegistryNames() {}

    public static Component block(Identifier id) {
        return BuiltInRegistries.BLOCK.get(id)
                .map(holder -> holder.value().getName())
                .orElseGet(() -> Component.literal(prettify(id)));
    }

    public static Component entity(Identifier id) {
        return BuiltInRegistries.ENTITY_TYPE.get(id)
                .map(holder -> (Component) holder.value().getDescription())
                .orElseGet(() -> Component.literal(prettify(id)));
    }

    public static Component biome(Identifier id) {
        return Component.translatableWithFallback(
                "biome." + id.getNamespace() + "." + id.getPath(), prettify(id));
    }

    public static Component structure(Identifier id, String fallback) {
        return Component.translatableWithFallback(
                "structure." + id.getNamespace() + "." + id.getPath(),
                fallback != null && !fallback.isEmpty() ? fallback : prettify(id));
    }

    /**
     * Formats a resource id path as a readable name ("village_plains" -> "Village Plains").
     * Used only when nothing better is available.
     */
    public static String prettify(Identifier id) {
        return capitalizeWords(id.getPath().replace('_', ' '));
    }

    public static String prettify(String resourceId) {
        int colon = resourceId.indexOf(':');
        String path = colon >= 0 ? resourceId.substring(colon + 1) : resourceId;
        return capitalizeWords(path.replace('_', ' '));
    }

    private static String capitalizeWords(String str) {
        StringBuilder result = new StringBuilder(str.length());
        boolean capitalizeNext = true;
        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
