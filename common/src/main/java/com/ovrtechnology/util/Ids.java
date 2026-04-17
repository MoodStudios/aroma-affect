package com.ovrtechnology.util;

import com.ovrtechnology.AromaAffect;
import net.minecraft.resources.ResourceLocation;

/**
 * Thin wrapper around {@link ResourceLocation} construction so that porting to
 * a new Minecraft version (e.g. 1.21.11 renamed {@code ResourceLocation} to
 * {@code Identifier}, and factory methods keep drifting) only requires touching
 * one file instead of every caller.
 * <p>
 * Keep this class free of logic — it is a pure indirection layer.
 */
public final class Ids {

    public static final String MOD_NAMESPACE = AromaAffect.MOD_ID;

    private Ids() {}

    /** Mod-owned identifier under the {@code aromaaffect:} namespace. */
    public static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_NAMESPACE, path);
    }

    /** Identifier under an arbitrary namespace. */
    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    /** Identifier under the vanilla {@code minecraft:} namespace. */
    public static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    /** Parses {@code namespace:path}; returns {@code null} on invalid input. */
    public static ResourceLocation tryParse(String id) {
        return ResourceLocation.tryParse(id);
    }

    /** Parses {@code namespace:path}; throws on invalid input. */
    public static ResourceLocation parse(String id) {
        return ResourceLocation.parse(id);
    }
}
