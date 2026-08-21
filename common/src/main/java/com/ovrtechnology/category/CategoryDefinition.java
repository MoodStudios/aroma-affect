package com.ovrtechnology.category;

import com.google.gson.annotations.SerializedName;
import com.ovrtechnology.definition.Definition;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * <p>Example JSON entry:</p>
 * <pre>
 * {
 *   "id": "STAT_BUFF",
 *   "color_html": "#5DECF5",
 *   "fallback_name": "Stat Buff",
 *   "mask": "aromaaffect:textures/mask/citrus",
 *   "loop": false
 * }
 * </pre>
 */

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class CategoryDefinition extends Definition {
    @SerializedName("mask")
    private String mask;

    /**
     * Whether the mask animation restarts once it reaches its last frame. Sheets
     * authored as a one-shot reveal set this to false: they play through and hold
     * the final frame instead of snapping back to the start mid-puff.
     */
    @SerializedName("loop")
    private Boolean loop;

    public CategoryDefinition() {
    }

    public CategoryDefinition(String id, String fallbackName, String colorHtml) {
        super(id, fallbackName, colorHtml);
    }

    /** Defaults to looping, which is what every sheet but a scripted reveal wants. */
    public boolean isMaskLooping() {
        return loop == null || loop;
    }

    @Override
    protected String getFallbackTitle() {
        return "Category";
    }

    @Override
    protected String getTranslationTitle() {
        return "category";
    }
}

