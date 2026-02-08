package com.ovrtechnology.nose.client;

import com.ovrtechnology.AromaAffect;

public final class NoseRenderToggles {
    private static boolean strapEnabled = false;
    private static boolean noseEnabled = true;

    private NoseRenderToggles() {
    }

    public static boolean isStrapEnabled() {
        return strapEnabled;
    }

    public static void setStrapEnabled(boolean enabled) {
        if (strapEnabled != enabled) {
            AromaAffect.LOGGER.info("[NoseToggle] strapEnabled: {} -> {} (caller: {})",
                    strapEnabled, enabled, getCaller());
        }
        strapEnabled = enabled;
    }

    public static boolean toggleStrapEnabled() {
        strapEnabled = !strapEnabled;
        AromaAffect.LOGGER.info("[NoseToggle] strapEnabled toggled to: {} (caller: {})",
                strapEnabled, getCaller());
        return strapEnabled;
    }

    public static boolean isNoseEnabled() {
        return noseEnabled;
    }

    public static void setNoseEnabled(boolean enabled) {
        if (noseEnabled != enabled) {
            AromaAffect.LOGGER.info("[NoseToggle] noseEnabled: {} -> {} (caller: {})",
                    noseEnabled, enabled, getCaller());
        }
        noseEnabled = enabled;
    }

    private static String getCaller() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // 0=getStackTrace, 1=getCaller, 2=setNoseEnabled/etc, 3=actual caller
        if (stack.length > 3) {
            StackTraceElement e = stack[3];
            return e.getClassName().substring(e.getClassName().lastIndexOf('.') + 1)
                    + "." + e.getMethodName() + ":" + e.getLineNumber();
        }
        return "unknown";
    }
}
