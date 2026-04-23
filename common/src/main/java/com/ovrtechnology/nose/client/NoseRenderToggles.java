package com.ovrtechnology.nose.client;

public final class NoseRenderToggles {
    private static boolean strapEnabled = false;
    private static boolean noseEnabled = true;

    private NoseRenderToggles() {}

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

    public static boolean isNoseEnabled() {
        return noseEnabled;
    }

    public static void setNoseEnabled(boolean enabled) {
        noseEnabled = enabled;
    }
}
