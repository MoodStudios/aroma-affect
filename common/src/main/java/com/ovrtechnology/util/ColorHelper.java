package com.ovrtechnology.util;

public class ColorHelper {
    /**
     * Parse the HTML color to an integer RGB value.
     *
     * @return RGB color as integer (0xRRGGBB format)
     */
    public static int getColorAsInt(String color) {
        try {
            // Remove the # prefix
            String hex = color.substring(1);

            // Handle short format (#RGB -> #RRGGBB)
            if (hex.length() == 3) {
                char r = hex.charAt(0);
                char g = hex.charAt(1);
                char b = hex.charAt(2);
                hex = "" + r + r + g + g + b + b;
            }

            // Parse only RGB portion (ignore alpha if present)
            if (hex.length() >= 6) {
                return Integer.parseInt(hex.substring(0, 6), 16);
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return 0xFFFFFF; // Default white
    }
}