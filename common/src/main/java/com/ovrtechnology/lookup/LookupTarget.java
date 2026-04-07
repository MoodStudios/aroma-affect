package com.ovrtechnology.lookup;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Represents a target to look up in the world.
 * Immutable record combining the type of lookup with the specific resource identifier.
 * 
 * @param type the type of lookup (biome, structure, or block)
 * @param resourceId the resource location identifying what to find
 */
public record LookupTarget(LookupType type, Identifier resourceId) {
    
    public LookupTarget {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(resourceId, "resourceId cannot be null");
    }
    
    /**
     * Creates a biome lookup target.
     */
    public static LookupTarget biome(Identifier biomeId) {
        return new LookupTarget(LookupType.BIOME, biomeId);
    }
    
    /**
     * Creates a structure lookup target.
     */
    public static LookupTarget structure(Identifier structureId) {
        return new LookupTarget(LookupType.STRUCTURE, structureId);
    }
    
    /**
     * Creates a block lookup target.
     */
    public static LookupTarget block(Identifier blockId) {
        return new LookupTarget(LookupType.BLOCK, blockId);
    }
    
    /**
     * Gets a unique cache key for this target.
     */
    public String getCacheKey() {
        return type.getId() + ":" + resourceId.toString();
    }
    
    @Override
    public String toString() {
        return type.getId() + "[" + resourceId + "]";
    }
}

