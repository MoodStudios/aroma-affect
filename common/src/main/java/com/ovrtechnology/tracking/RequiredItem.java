package com.ovrtechnology.tracking;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an item required to track a specific target.
 * Deserialized from JSON definitions (blocks, structures, etc.).
 */
public class RequiredItem {

    @SerializedName("item_id")
    private String itemId;

    @SerializedName("count")
    private int count = 1;

    public RequiredItem() {
    }

    public RequiredItem(String itemId, int count) {
        this.itemId = itemId;
        this.count = count;
    }

    public String getItemId() {
        return itemId;
    }

    public int getCount() {
        return count > 0 ? count : 1;
    }
}
