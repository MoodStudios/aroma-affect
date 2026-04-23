package com.ovrtechnology.nose;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class NoseUnlock {

    @SerializedName("blocks")
    private List<String> blocks;

    @SerializedName("biomes")
    private List<String> biomes;

    @SerializedName("structures")
    private List<String> structures;

    @SerializedName("flowers")
    private List<String> flowers;

    @SerializedName("abilities")
    private List<String> abilities;

    @SerializedName("noses")
    private List<String> noses;

    public List<String> getBlocks() {
        return blocks != null ? blocks : Collections.emptyList();
    }

    public List<String> getBiomes() {
        return biomes != null ? biomes : Collections.emptyList();
    }

    public List<String> getStructures() {
        return structures != null ? structures : Collections.emptyList();
    }

    public List<String> getFlowers() {
        return flowers != null ? flowers : Collections.emptyList();
    }

    public List<String> getAbilities() {
        return abilities != null ? abilities : Collections.emptyList();
    }

    public List<String> getNoses() {
        return noses != null ? noses : Collections.emptyList();
    }

    public boolean hasBlockUnlocks() {
        return blocks != null && !blocks.isEmpty();
    }

    public boolean hasBiomeUnlocks() {
        return biomes != null && !biomes.isEmpty();
    }

    public boolean hasStructureUnlocks() {
        return structures != null && !structures.isEmpty();
    }

    public boolean hasFlowerUnlocks() {
        return flowers != null && !flowers.isEmpty();
    }

    public boolean hasAbilityUnlocks() {
        return abilities != null && !abilities.isEmpty();
    }

    public boolean hasNoseInheritance() {
        return noses != null && !noses.isEmpty();
    }

    public boolean hasAnyUnlocks() {
        return hasBlockUnlocks()
                || hasBiomeUnlocks()
                || hasStructureUnlocks()
                || hasFlowerUnlocks()
                || hasAbilityUnlocks()
                || hasNoseInheritance();
    }
}
