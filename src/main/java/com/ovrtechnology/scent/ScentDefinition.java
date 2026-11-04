package com.ovrtechnology.scent;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.definition.Definition;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Represents a scent definition loaded from JSON.
 * 
 * <p>Each scent corresponds to an OVR hardware scent identifier and has a fallback
 * display name for when localization is not available. The actual display name
 * should be retrieved from Minecraft's localization system using the key format:
 * {@code scent.aromaaffect.<id>}</p>
 * 
 * <p>Example JSON entry:</p>
 * <pre>
 * {
 *   "id": "winter",
 *   "fallback_name": "Winter"
 * }
 * </pre>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class ScentDefinition extends Definition {

    /**
     * Optional description of the scent for tooltips.
     * If not provided, falls back to localization key: scent.aromaaffect.{id}.description
     */
    @SerializedName("description")
    private String description;

    @SerializedName("mask")
    private String mask;

    /**
     * Default constructor for GSON deserialization.
     */
    public ScentDefinition() {
        super();
    }

    /**
     * Constructor for programmatic creation.
     *
     * @param id Unique scent identifier
     * @param fallbackName Fallback display name
     */
    public ScentDefinition(String id, String fallbackName, String colorHTML) {
        super(id, fallbackName, colorHTML);
    }

    /**
     * Get the localization key for this scent's name.
     *
     * @return The localization key in format "scent.aromaaffect.{id}"
     */
    @Override
    public String getTranslationKey() {
        return "scent.aromaaffect." + id;
    }

    /**
     * Get the localization key for this scent's description.
     *
     * @return The localization key in format "scent.aromaaffect.{id}.description"
     */
    public String getDescriptionTranslationKey() {
        return "scent.aromaaffect." + id + ".description";
    }
}

