package com.ovrtechnology.lookup;

/**
 * Types of things that can be looked up in the world.
 * Each type uses different Minecraft APIs and optimization strategies.
 */
public enum LookupType {
    /**
     * Biome lookup using ServerLevel.findClosestBiome3d().
     * Relatively fast, uses Minecraft's native biome sampling.
     */
    BIOME("biome"),
    
    /**
     * Structure lookup using ServerLevel.findNearestMapStructure().
     * Can be expensive as it may trigger chunk generation.
     */
    STRUCTURE("structure"),
    
    /**
     * Block lookup using custom spiral chunk scanning.
     * Most expensive operation - uses async processing.
     */
    BLOCK("block");
    
    private final String id;
    
    LookupType(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    /**
     * Gets a LookupType from its string ID.
     * 
     * @param id the type ID
     * @return the LookupType, or null if not found
     */
    public static LookupType fromId(String id) {
        for (LookupType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}

