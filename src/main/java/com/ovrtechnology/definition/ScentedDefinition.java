package com.ovrtechnology.definition;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.tracking.RequiredItem;
import com.ovrtechnology.trigger.ScentPriority;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class ScentedDefinition extends Definition {
    /**
     * Reference to a scent ID from scents.json.
     * This scent will be emitted when tracking or entering this biome.
     */
    @SerializedName("scent_id")
    @Getter
    protected String scentId;

    /**
     * Priority level for this trigger.
     */
    @SerializedName("priority")
    @Getter
    private ScentPriority priority = getDefaultPriority();

    /**
     * Scent intensity (0.0 to 1.0).
     * If not specified, uses the global biome_intensity from settings.
     */
    @SerializedName("intensity")
    @Getter
    private Double intensity;

    /**
     * Range in blocks for proximity triggers.
     */
    @SerializedName("range")
    @Getter
    private int range = getDefaultRange();

    /**
     * Durability cost for tracking this specific block.
     * If null or <= 0, defaults to 10.
     */
    @Getter(AccessLevel.NONE)
    @SerializedName("track_cost")
    private Integer trackCost;

    /**
     * Optional item required from the player's inventory to track this block.
     * If null, no item is required.
     */
    @Getter(AccessLevel.NONE)
    @SerializedName("required_item")
    private RequiredItem requiredItem;

    public ScentedDefinition() {
    }

    /**
     * Constructor for programmatic creation.
     *
     * @param id Minecraft ID
     * @param colorHtml HTML hex color
     */
    public ScentedDefinition(String id, String fallbackName, String colorHtml, String scentId) {
        super(id, fallbackName, colorHtml);
        this.scentId = scentId;
    }

    protected ScentPriority getDefaultPriority() {
        return ScentPriority.LOW;
    }

    /**
     * Check if the scent_id field is set.
     *
     * @return true if scent_id is specified
     */
    public boolean hasScentId() {
        return scentId != null && !scentId.isEmpty();
    }

    public int getTrackCost() {
        return trackCost != null && trackCost > 0 ? trackCost : 10;
    }

    public RequiredItem getRequiredItem() { return requiredItem; }

    protected int getDefaultRange() {
        return 5;
    }
}
