package com.ovrtechnology.nose;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the unlock conditions for a nose.
 * A nose can unlock detection of blocks, biomes, structures, abilities,
 * and can also inherit abilities from other noses.
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class NoseUnlock {
    
    /**
     * List of block IDs this nose can detect (e.g., "minecraft:water", "minecraft:lava", "minecraft:iron_ore")
     */
    @SerializedName("blocks")
    private List<String> blocks;
    
    /**
     * List of biome IDs this nose can detect (e.g., "minecraft:plains", "minecraft:snowy_plains")
     */
    @SerializedName("biomes")
    private List<String> biomes;
    
    /**
     * List of structure IDs this nose can detect (e.g., "minecraft:dungeon", "minecraft:ancient_city")
     */
    @SerializedName("structures")
    private List<String> structures;
    
    /**
     * List of ability IDs this nose unlocks (references to ability definitions in code)
     */
    @SerializedName("abilities")
    private List<String> abilities;
    
    /**
     * List of nose IDs to inherit abilities from.
     * This allows a nose to have all abilities from referenced noses.
     * Circular dependencies are detected and prevented.
     */
    @SerializedName("noses")
    private List<String> noses;
    
    // Getters with null safety (override Lombok for null safety)
    
    public List<String> getBlocks() {
        return blocks != null ? blocks : Collections.emptyList();
    }
    
    public List<String> getBiomes() {
        return biomes != null ? biomes : Collections.emptyList();
    }
    
    public List<String> getStructures() {
        return structures != null ? structures : Collections.emptyList();
    }
    
    public List<String> getAbilities() {
        return abilities != null ? abilities : Collections.emptyList();
    }
    
    public List<String> getNoses() {
        return noses != null ? noses : Collections.emptyList();
    }
    
    /**
     * Check if this unlock has any block detection
     */
    public boolean hasBlockUnlocks() {
        return blocks != null && !blocks.isEmpty();
    }
    
    /**
     * Check if this unlock has any biome detection
     */
    public boolean hasBiomeUnlocks() {
        return biomes != null && !biomes.isEmpty();
    }
    
    /**
     * Check if this unlock has any structure detection
     */
    public boolean hasStructureUnlocks() {
        return structures != null && !structures.isEmpty();
    }
    
    /**
     * Check if this unlock has any ability unlocks
     */
    public boolean hasAbilityUnlocks() {
        return abilities != null && !abilities.isEmpty();
    }
    
    /**
     * Check if this unlock inherits from other noses
     */
    public boolean hasNoseInheritance() {
        return noses != null && !noses.isEmpty();
    }
    
    /**
     * Check if this unlock has any conditions at all
     */
    public boolean hasAnyUnlocks() {
        return hasBlockUnlocks() || hasBiomeUnlocks() || hasStructureUnlocks() || hasAbilityUnlocks() || hasNoseInheritance();
    }
}
