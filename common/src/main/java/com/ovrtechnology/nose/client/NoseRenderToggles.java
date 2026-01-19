package com.ovrtechnology.nose.client;

public final class NoseRenderToggles {
    private static boolean strapEnabled = false;

    private NoseRenderToggles() {
    }

    public static boolean isStrapEnabled() {
        return strapEnabled;
    }

    public static void setStrapEnabled(boolean enabled) {
        strapEnabled = enabled;
    }

    public static boolean toggleStrapEnabled() {
        strapEnabled = !strapEnabled;
        return strapEnabled;
    }
}
