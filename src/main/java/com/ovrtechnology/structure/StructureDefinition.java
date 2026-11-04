package com.ovrtechnology.structure;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.definition.ImageDefinition;
import com.ovrtechnology.tracking.RequiredItem;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a trackable structure definition loaded from JSON.
 * 
 * <p>Each structure definition maps a Minecraft structure to a scent and provides
 * display properties for UI rendering (color, image, name).</p>
 * 
 * <p>Structures can also specify a list of characteristic blocks that help
 * identify the structure type (e.g., end portal frames for strongholds).</p>
 * 
 * <p>Example JSON entry:</p>
 * <pre>
 * {
 *   "structure_id": "minecraft:stronghold",
 *   "image": "structure/stronghold",
 *   "fallback_name": "Stronghold",
 *   "color_html": "#4A4A4A",
 *   "scent_id": "terra_silva",
 *   "blocks": [
 *     "minecraft:end_portal_frame",
 *     "minecraft:stone_bricks"
 *   ]
 * }
 * </pre>
 * 
 * <h2>Structure ID Format</h2>
 * <p>Structure IDs follow the Minecraft Identifier format: {@code namespace:path}.
 * For vanilla structures, use {@code minecraft:} namespace. For modded structures,
 * use the mod's namespace (e.g., {@code create:contraption_base}).</p>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class StructureDefinition extends ImageDefinition {
    /**
     * List of block IDs that are characteristic of this structure.
     * Can be used for structure identification or as tracking hints.
     * These should reference valid Minecraft block IDs.
     */
    @SerializedName("blocks")
    private List<String> blocks;

    /**
     * Default constructor for GSON deserialization.
     */
    public StructureDefinition() {
        this.blocks = new ArrayList<>();
    }

    /**
     * Constructor for programmatic creation.
     *
     * @param id           Minecraft structure ID
     * @param fallbackName Display name fallback
     * @param colorHtml    HTML hex color
     * @param scentId      Reference to scent definition
     */
    public StructureDefinition(String id, String fallbackName, String colorHtml, String scentId) {
        super(id, fallbackName, colorHtml, scentId);
        this.blocks = new ArrayList<>();
    }

    /**
     * Get the blocks list with null safety.
     *
     * @return List of block IDs, never null
     */
    public List<String> getBlocks() {
        return blocks != null ? blocks : Collections.emptyList();
    }

    /**
     * Check if this structure has associated blocks.
     *
     * @return true if blocks list is non-empty
     */
    public boolean hasBlocks() {
        return blocks != null && !blocks.isEmpty();
    }

    /**
     * Get the path portion of the structure ID.
     *
     * @return Path (e.g., "stronghold"), or the full ID if no namespace
     */
    public String getPath() {
        if (id != null && id.contains(":")) {
            return id.split(":", 2)[1];
        }
        return id != null ? id : "";
    }

    @Override
    protected String getDefaultColor() {
        return "#808080";
    }

    @Override
    protected String getTranslationTitle() {
        return "structure";
    }

    @Override
    protected String getFallbackTitle() {
        return "Structure";
    }

    @Override
    protected String getDefaultMode() {
        return "PROXIMITY";
    }

    @Override
    protected int getDefaultRange() {
        return 50;
    }

    @Override
    protected ScentPriority getDefaultPriority() {
        return ScentPriority.MEDLOW;
    }
}
