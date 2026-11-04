package com.ovrtechnology.util;

import com.ovrtechnology.AromaAffect;
import net.minecraft.resources.Identifier;

public final class Ids {

    public static final String MOD_NAMESPACE = AromaAffect.MOD_ID;

    private Ids() {}

    public static Identifier mod(String path) {
        return Identifier.fromNamespaceAndPath(MOD_NAMESPACE, path);
    }

    public static Identifier of(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier vanilla(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    public static Identifier tryParse(String id) {
        return Identifier.tryParse(id);
    }

    public static Identifier parse(String id) {
        return Identifier.parse(id);
    }
}
