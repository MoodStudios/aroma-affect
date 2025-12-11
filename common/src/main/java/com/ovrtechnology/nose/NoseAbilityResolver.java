package com.ovrtechnology.nose;

import com.ovrtechnology.AromaCraft;
import lombok.Getter;

import java.util.*;

/**
 * Resolves and caches all abilities for each nose, including inherited abilities from other noses.
 * Handles circular dependency detection to prevent infinite loops.
 */
public final class NoseAbilityResolver {
    
    /**
     * Cached resolved abilities for each nose ID.
     * Key: nose ID, Value: set of all ability IDs (including inherited)
     */
    private static final Map<String, ResolvedAbilities> ABILITY_CACHE = new HashMap<>();
    
    /**
     * Whether the resolver has been initialized
     */
    private static boolean initialized = false;
    
    /**
     * Initialize the ability resolver.
     * Must be called after all nose definitions are loaded.
     */
    public static void init() {
        if (initialized) {
            AromaCraft.LOGGER.warn("NoseAbilityResolver.init() called multiple times!");
            return;
        }
        
        AromaCraft.LOGGER.info("Initializing NoseAbilityResolver...");
        
        // Resolve abilities for each nose
        for (String noseId : NoseRegistry.getAllNoseIds()) {
            resolveAbilitiesForNose(noseId);
        }
        
        initialized = true;
        AromaCraft.LOGGER.info("NoseAbilityResolver initialized with {} cached entries", ABILITY_CACHE.size());
    }
    
    /**
     * Resolve and cache all abilities for a nose (including inherited ones)
     */
    private static ResolvedAbilities resolveAbilitiesForNose(String noseId) {
        // Check cache first
        if (ABILITY_CACHE.containsKey(noseId)) {
            return ABILITY_CACHE.get(noseId);
        }
        
        // Create a new resolution context with visited tracking for circular dependency detection
        Set<String> visited = new HashSet<>();
        Set<String> abilities = new LinkedHashSet<>();
        Set<String> blocks = new LinkedHashSet<>();
        Set<String> biomes = new LinkedHashSet<>();
        Set<String> structures = new LinkedHashSet<>();
        List<String> circularDependencies = new ArrayList<>();
        
        resolveRecursive(noseId, visited, abilities, blocks, biomes, structures, circularDependencies);
        
        // Report any circular dependencies found
        if (!circularDependencies.isEmpty()) {
            AromaCraft.LOGGER.warn("Circular dependencies detected for nose '{}': {}", noseId, circularDependencies);
        }
        
        ResolvedAbilities resolved = new ResolvedAbilities(
                Collections.unmodifiableSet(abilities),
                Collections.unmodifiableSet(blocks),
                Collections.unmodifiableSet(biomes),
                Collections.unmodifiableSet(structures)
        );
        
        ABILITY_CACHE.put(noseId, resolved);
        AromaCraft.LOGGER.debug("Resolved abilities for '{}': {} abilities, {} blocks, {} biomes, {} structures",
                noseId, abilities.size(), blocks.size(), biomes.size(), structures.size());
        
        return resolved;
    }
    
    /**
     * Recursively resolve abilities, detecting circular dependencies
     */
    private static void resolveRecursive(
            String noseId,
            Set<String> visited,
            Set<String> abilities,
            Set<String> blocks,
            Set<String> biomes,
            Set<String> structures,
            List<String> circularDependencies
    ) {
        // Circular dependency check
        if (visited.contains(noseId)) {
            circularDependencies.add(noseId);
            AromaCraft.LOGGER.warn("Circular dependency detected: nose '{}' already in resolution chain", noseId);
            return;
        }
        
        visited.add(noseId);
        
        // Get the nose definition
        Optional<NoseDefinition> defOpt = NoseRegistry.getDefinition(noseId);
        if (defOpt.isEmpty()) {
            AromaCraft.LOGGER.warn("Referenced nose '{}' not found during ability resolution", noseId);
            visited.remove(noseId);
            return;
        }
        
        NoseDefinition definition = defOpt.get();
        NoseUnlock unlock = definition.getUnlock();
        
        if (unlock == null) {
            visited.remove(noseId);
            return;
        }
        
        // Add direct abilities
        abilities.addAll(unlock.getAbilities());
        blocks.addAll(unlock.getBlocks());
        biomes.addAll(unlock.getBiomes());
        structures.addAll(unlock.getStructures());
        
        // Recursively resolve inherited noses
        for (String inheritedNoseId : unlock.getNoses()) {
            resolveRecursive(inheritedNoseId, visited, abilities, blocks, biomes, structures, circularDependencies);
        }
        
        visited.remove(noseId);
    }
    
    /**
     * Get all resolved abilities for a nose (including inherited)
     */
    public static ResolvedAbilities getResolvedAbilities(String noseId) {
        if (!initialized) {
            AromaCraft.LOGGER.error("NoseAbilityResolver not initialized! Call init() first.");
            return ResolvedAbilities.EMPTY;
        }
        
        ResolvedAbilities resolved = ABILITY_CACHE.get(noseId);
        if (resolved == null) {
            AromaCraft.LOGGER.warn("No resolved abilities found for nose: {}", noseId);
            return ResolvedAbilities.EMPTY;
        }
        
        return resolved;
    }
    
    /**
     * Check if a nose has a specific ability (including inherited)
     */
    public static boolean hasAbility(String noseId, String abilityId) {
        return getResolvedAbilities(noseId).getAbilities().contains(abilityId);
    }
    
    /**
     * Check if a nose can detect a specific block (including inherited)
     */
    public static boolean canDetectBlock(String noseId, String blockId) {
        return getResolvedAbilities(noseId).getBlocks().contains(blockId);
    }
    
    /**
     * Check if a nose can detect a specific biome (including inherited)
     */
    public static boolean canDetectBiome(String noseId, String biomeId) {
        return getResolvedAbilities(noseId).getBiomes().contains(biomeId);
    }
    
    /**
     * Check if a nose can detect a specific structure (including inherited)
     */
    public static boolean canDetectStructure(String noseId, String structureId) {
        return getResolvedAbilities(noseId).getStructures().contains(structureId);
    }
    
    /**
     * Clear the ability cache (for reloading)
     */
    public static void clearCache() {
        ABILITY_CACHE.clear();
        initialized = false;
        AromaCraft.LOGGER.info("NoseAbilityResolver cache cleared");
    }
    
    /**
     * Container for resolved abilities
     */
    @Getter
    public static class ResolvedAbilities {
        public static final ResolvedAbilities EMPTY = new ResolvedAbilities(
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet()
        );
        
        private final Set<String> abilities;
        private final Set<String> blocks;
        private final Set<String> biomes;
        private final Set<String> structures;
        
        public ResolvedAbilities(Set<String> abilities, Set<String> blocks, Set<String> biomes, Set<String> structures) {
            this.abilities = abilities;
            this.blocks = blocks;
            this.biomes = biomes;
            this.structures = structures;
        }
        
        public boolean hasAbility(String abilityId) {
            return abilities.contains(abilityId);
        }
        
        public boolean canDetectBlock(String blockId) {
            return blocks.contains(blockId);
        }
        
        public boolean canDetectBiome(String biomeId) {
            return biomes.contains(biomeId);
        }
        
        public boolean canDetectStructure(String structureId) {
            return structures.contains(structureId);
        }
    }
}
