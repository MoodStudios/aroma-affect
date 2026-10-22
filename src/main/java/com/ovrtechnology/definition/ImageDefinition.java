package com.ovrtechnology.definition;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.regex.Pattern;

public abstract class ImageDefinition extends ScentedDefinition {
    /**
     * Pattern for validating Identifier format (namespace:path).
     */
    private static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    /**
     * Path to the texture file relative to assets/{MOD_ID}/textures/
     * Used for displaying definition icons in menus.
     */
    @SerializedName("image")
    private String image;

    /**
     * Trigger mode: "ENTER" (once on entry) or "AMBIENT" (continuous).
     */
    @SerializedName("mode")
    @Getter
    private String mode = getDefaultMode();

    public ImageDefinition() {
        super();
    }

    public ImageDefinition(String id, String fallbackName, String colorHtml, String scentId) {
        super(id, fallbackName, colorHtml, scentId);
    }

    /**
     * Get the image path with fallback.
     *
     * @return Image path, or default if not set
     */
    public String getImage() {
        return (image != null && !image.isEmpty()) ? image : getDefaultImage(this);
    }

    /**
     * Get the raw image value without fallback.
     *
     * @return The raw image value as stored
     */
    public String getRawImage() {
        return image;
    }

    /**
     * Get the namespace portion of the definition ID.
     *
     * @return Namespace (e.g., "minecraft"), or empty string if invalid
     */
    public String getNamespace() {
        if (id != null && id.contains(":")) {
            return id.split(":", 2)[0];
        }
        return "";
    }

    /**
     * Check if this is a vanilla Minecraft definition.
     *
     * @return true if namespace is "minecraft"
     */
    public boolean isVanilla() {
        return "minecraft".equals(getNamespace());
    }

    /**
     * Check if the id has valid Identifier format.
     *
     * @return true if id matches namespace:path format
     */
    public boolean hasValidIDFormat() {
        return id != null && RESOURCE_LOCATION_PATTERN.matcher(id).matches();
    }

    /**
     * Validate if a string is a valid Identifier format.
     *
     * @param resourceLocation The resource location to validate
     * @return true if valid namespace:path format
     */
    public static boolean isValidIdentifier(String resourceLocation) {
        if (resourceLocation == null || resourceLocation.isEmpty()) {
            return false;
        }
        return RESOURCE_LOCATION_PATTERN.matcher(resourceLocation).matches();
    }

    public static String getDefaultImage(ImageDefinition instance) {
        return instance.getTranslationTitle() + "/unknown";
    }

    protected String getDefaultMode() {
        return "AMBIENT";
    }
}
