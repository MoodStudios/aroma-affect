package com.ovrtechnology.util;

import com.ovrtechnology.AromaAffect;
import net.minecraft.resources.ResourceLocation;

public final class Ids {

    public static final String MOD_NAMESPACE = AromaAffect.MOD_ID;

    private Ids() {}

    public static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_NAMESPACE, path);
    }

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    public static ResourceLocation tryParse(String id) {
        return ResourceLocation.tryParse(id);
    }

    public static ResourceLocation parse(String id) {
        return ResourceLocation.parse(id);
    }
}
