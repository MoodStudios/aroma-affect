package com.ovrtechnology.sniffer.loot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.util.RandomSource;

@Getter
@Setter
@Accessors(chain = true)
public class IntRange {

    @SerializedName("min")
    private Integer min;

    @SerializedName("max")
    private Integer max;

    public int getMinOrDefault() {
        return min != null ? min : 1;
    }

    public int getMaxOrDefault() {
        return max != null ? max : getMinOrDefault();
    }

    public int sample(RandomSource random) {
        int lo = getMinOrDefault();
        int hi = getMaxOrDefault();
        if (hi <= lo) return lo;
        return lo + random.nextInt(hi - lo + 1);
    }
}
