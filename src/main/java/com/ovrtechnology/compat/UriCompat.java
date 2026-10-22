package com.ovrtechnology.compat;

import java.net.URI;

public final class UriCompat {

    private UriCompat() {}

    public static void open(URI uri) {
        //? if >=26.3 {
        com.mojang.blaze3d.Blaze3D.openUri(uri);
        //?} else {
        /*net.minecraft.util.Util.getPlatform().openUri(uri);
        *///?}
    }

    public static void open(String uri) {
        open(URI.create(uri));
    }
}
