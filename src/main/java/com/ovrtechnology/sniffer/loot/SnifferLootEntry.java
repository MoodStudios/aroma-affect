package com.ovrtechnology.sniffer.loot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class SnifferLootEntry {

    @SerializedName("item")
    private String item;

    @SerializedName("tag")
    private String tag;

    @SerializedName("loot_table")
    private String lootTable;

    @SerializedName("weight")
    private Integer weight;

    @SerializedName("count")
    private IntRange count;

    public int getWeightOrDefault() {
        return weight != null && weight > 0 ? weight : 1;
    }
}
