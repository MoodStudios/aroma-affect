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
 *   "mask": "aromaaffect:textures/mask/citrus"
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

    public CategoryDefinition() {
    }

    public CategoryDefinition(String id, String fallbackName, String colorHtml) {
        super(id, fallbackName, colorHtml);
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

