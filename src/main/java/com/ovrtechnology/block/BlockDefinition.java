package com.ovrtechnology.block;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.definition.ScentedDefinition;
import com.ovrtechnology.tracking.RequiredItem;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

/**
 * Represents a trackable block definition loaded from JSON.
 *
 * <p>Each block definition maps a Minecraft block to a scent and provides
 * a display color for UI rendering (e.g., in the radial menu or compass).</p>
 *
 * <p>Example JSON entry:</p>
 * <pre>
 * {
 *   "id": "minecraft:diamond_ore",
 *   "color_html": "#5DECF5",
 *   "scent_id": "terra_silva"
 * }
 * </pre>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class BlockDefinition extends ScentedDefinition {
    /**
     * Trigger mode: "INTERACT" (on right-click) or "PROXIMITY" (when near).
     */
    @SerializedName("trigger_on")
    private String triggerOn = "PROXIMITY";

    /**
     * Default constructor for GSON deserialization.
     */
    public BlockDefinition() {
        super();
    }

    /**
     * Constructor for programmatic creation.
     *
     * @param id Minecraft block ID
     * @param colorHtml HTML hex color
     * @param scentId Reference to scent definition
     */
    public BlockDefinition(String id, String fallbackName, String colorHtml, String scentId) {
        super(id, fallbackName, colorHtml, scentId);
    }

    @Override
    protected String getFallbackTitle() {
        return "Block";
    }

    @Override
    protected String getTranslationTitle() {
        return "block";
    }
}
