package com.ovrtechnology.legacy.util;

public final class ARGB {

    private ARGB() {}

    public static int color(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
    }

    public static int color(int red, int green, int blue) {
        return color(255, red, green, blue);
    }

    public static int color(float alpha, int rgb) {
        return as8BitChannel(alpha) << 24 | (rgb & 0xFFFFFF);
    }

    public static int color(int alpha, int rgb) {
        return (alpha & 0xFF) << 24 | (rgb & 0xFFFFFF);
    }

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & 0xFF;
    }

    public static int green(int color) {
        return color >> 8 & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int as8BitChannel(float value) {
        return (int) Math.floor(Math.max(0.0F, Math.min(1.0F, value)) * 255.0F);
    }
}
