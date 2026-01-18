package com.ovrtechnology.trigger;

import com.google.gson.annotations.SerializedName;

/**
 * Priority levels for scent triggers.
 * 
 * <p>Based on OVR's priority system. Higher priority scents override lower ones.
 * When two scents have the same priority, the most recently triggered one wins.</p>
 * 
 * <h3>Priority Levels:</h3>
 * <ul>
 *   <li><b>HIGH</b> - Hero/critical scents that override everything (item use, healing)</li>
 *   <li><b>MEDIUM</b> - Important events (low health warning, block interactions)</li>
 *   <li><b>MEDLOW</b> - Ambient but noticeable (biome ambient, nearby mobs)</li>
 *   <li><b>LOW</b> - Background/subtle effects (reserved for future use)</li>
 * </ul>
 */
public enum ScentPriority {
    
    /**
     * Hero/critical scents - override everything else.
     * Examples: Item use triggers, healing effects.
     */
    @SerializedName("HIGH")
    HIGH(4),
    
    /**
     * Important events - medium-high priority.
     * Examples: Low health warning (Machina), block interactions.
     */
    @SerializedName("MEDIUM")
    MEDIUM(3),
    
    /**
     * Ambient but noticeable - medium-low priority.
     * Examples: Biome ambient scents, nearby animals.
     */
    @SerializedName("MEDLOW")
    MEDLOW(2),
    
    /**
     * Background/subtle - lowest priority.
     * Reserved for future fine-grained effects.
     */
    @SerializedName("LOW")
    LOW(1);
    
    private final int value;
    
    ScentPriority(int value) {
        this.value = value;
    }
    
    /**
     * Gets the numeric value of this priority.
     * Higher values mean higher priority.
     * 
     * @return the priority value (1-4)
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Compares this priority to another.
     * 
     * @param other the other priority to compare
     * @return positive if this is higher, negative if lower, 0 if equal
     */
    public int comparePriority(ScentPriority other) {
        return Integer.compare(this.value, other.value);
    }
    
    /**
     * Checks if this priority is higher than another.
     * 
     * @param other the other priority
     * @return true if this priority is higher
     */
    public boolean isHigherThan(ScentPriority other) {
        return this.value > other.value;
    }
    
    /**
     * Checks if this priority is lower than another.
     * 
     * @param other the other priority
     * @return true if this priority is lower
     */
    public boolean isLowerThan(ScentPriority other) {
        return this.value < other.value;
    }
}
