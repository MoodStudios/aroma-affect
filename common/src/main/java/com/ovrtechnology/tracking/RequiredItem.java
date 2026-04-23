package com.ovrtechnology.tracking;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

public class RequiredItem {

    @Getter
    @SerializedName("item_id")
    private String itemId;

    @SerializedName("count")
    private int count = 1;

    public int getCount() {
        return count > 0 ? count : 1;
    }
}
