package com.ovrtechnology.nose;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.variant.NoseVariant;
import com.ovrtechnology.variant.NoseVariantRegistry;
import java.util.*;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

public final class NoseAbilityResolver {

    private static final Map<String, ResolvedAbilities> ABILITY_CACHE = new HashMap<>();

    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            AromaAffect.LOGGER.warn("NoseAbilityResolver.init() called multiple times!");
            return;
        }

        AromaAffect.LOGGER.info("Initializing NoseAbilityResolver...");

        for (String noseId : NoseRegistry.getAllNoseIds()) {
            resolveAbilitiesForNose(noseId);
        }

        for (ResourceLocation variantId : NoseVariantRegistry.all().keySet()) {
            resolveAbilitiesForNose(variantId.toString());
        }

        initialized = true;
        AromaAffect.LOGGER.info(
                "NoseAbilityResolver initialized with {} cached entries", ABILITY_CACHE.size());
    }

    private static ResolvedAbilities resolveAbilitiesForNose(String noseId) {

        if (ABILITY_CACHE.containsKey(noseId)) {
            return ABILITY_CACHE.get(noseId);
        }

        Set<String> visited = new HashSet<>();
        Set<String> abilities = new LinkedHashSet<>();
        Set<String> blocks = new LinkedHashSet<>();
        Set<String> biomes = new LinkedHashSet<>();
        Set<String> structures = new LinkedHashSet<>();
        Set<String> flowers = new LinkedHashSet<>();
        List<String> circularDependencies = new ArrayList<>();

        resolveRecursive(
                noseId,
                visited,
                abilities,
                blocks,
                biomes,
                structures,
                flowers,
                circularDependencies);

        if (!circularDependencies.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "Circular dependencies detected for nose '{}': {}",
                    noseId,
                    circularDependencies);
        }

        ResolvedAbilities resolved =
                new ResolvedAbilities(
                        Collections.unmodifiableSet(abilities),
                        Collections.unmodifiableSet(blocks),
                        Collections.unmodifiableSet(biomes),
                        Collections.unmodifiableSet(structures),
                        Collections.unmodifiableSet(flowers));

        ABILITY_CACHE.put(noseId, resolved);
        AromaAffect.LOGGER.debug(
                "Resolved abilities for '{}': {} abilities, {} blocks, {} biomes, {} structures, {} flowers",
                noseId,
                abilities.size(),
                blocks.size(),
                biomes.size(),
                structures.size(),
                flowers.size());

        return resolved;
    }

    private static void resolveRecursive(
            String noseId,
            Set<String> visited,
            Set<String> abilities,
            Set<String> blocks,
            Set<String> biomes,
            Set<String> structures,
            Set<String> flowers,
            List<String> circularDependencies) {

        if (visited.contains(noseId)) {
            circularDependencies.add(noseId);
            AromaAffect.LOGGER.warn(
                    "Circular dependency detected: nose '{}' already in resolution chain", noseId);
            return;
        }

        visited.add(noseId);

        NoseUnlock unlock = lookupUnlock(noseId);
        if (unlock == null) {
            AromaAffect.LOGGER.warn(
                    "Referenced nose '{}' not found during ability resolution", noseId);
            visited.remove(noseId);
            return;
        }

        abilities.addAll(unlock.getAbilities());
        blocks.addAll(unlock.getBlocks());
        biomes.addAll(unlock.getBiomes());
        structures.addAll(unlock.getStructures());
        flowers.addAll(unlock.getFlowers());

        for (String inheritedNoseId : unlock.getNoses()) {
            resolveRecursive(
                    inheritedNoseId,
                    visited,
                    abilities,
                    blocks,
                    biomes,
                    structures,
                    flowers,
                    circularDependencies);
        }

        visited.remove(noseId);
    }

    private static NoseUnlock lookupUnlock(String noseId) {
        Optional<NoseDefinition> defOpt = NoseRegistry.getDefinition(noseId);
        if (defOpt.isPresent()) {
            return defOpt.get().getUnlock();
        }
        ResourceLocation rl = ResourceLocation.tryParse(noseId);
        if (rl != null) {
            Optional<NoseVariant> variantOpt = NoseVariantRegistry.get(rl);
            if (variantOpt.isPresent()) {
                return variantOpt.get().getUnlock();
            }
        }
        return null;
    }

    public static ResolvedAbilities getResolvedAbilities(String noseId) {
        if (!initialized) {
            AromaAffect.LOGGER.error("NoseAbilityResolver not initialized! Call init() first.");
            return ResolvedAbilities.EMPTY;
        }

        ResolvedAbilities resolved = ABILITY_CACHE.get(noseId);
        if (resolved == null) {
            AromaAffect.LOGGER.warn("No resolved abilities found for nose: {}", noseId);
            return ResolvedAbilities.EMPTY;
        }

        return resolved;
    }

    public static boolean hasAbility(String noseId, String abilityId) {
        return getResolvedAbilities(noseId).getAbilities().contains(abilityId);
    }

    public static boolean canDetectBlock(String noseId, String blockId) {
        return getResolvedAbilities(noseId).getBlocks().contains(blockId);
    }

    public static boolean canDetectBiome(String noseId, String biomeId) {
        return getResolvedAbilities(noseId).getBiomes().contains(biomeId);
    }

    public static boolean canDetectStructure(String noseId, String structureId) {
        return getResolvedAbilities(noseId).getStructures().contains(structureId);
    }

    public static boolean canDetectFlower(String noseId, String flowerId) {
        return getResolvedAbilities(noseId).getFlowers().contains(flowerId);
    }

    public static void clearCache() {
        ABILITY_CACHE.clear();
        initialized = false;
        AromaAffect.LOGGER.info("NoseAbilityResolver cache cleared");
    }

    @Getter
    public static class ResolvedAbilities {
        public static final ResolvedAbilities EMPTY =
                new ResolvedAbilities(
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet());

        private final Set<String> abilities;
        private final Set<String> blocks;
        private final Set<String> biomes;
        private final Set<String> structures;
        private final Set<String> flowers;

        public ResolvedAbilities(
                Set<String> abilities,
                Set<String> blocks,
                Set<String> biomes,
                Set<String> structures,
                Set<String> flowers) {
            this.abilities = abilities;
            this.blocks = blocks;
            this.biomes = biomes;
            this.structures = structures;
            this.flowers = flowers;
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

        public boolean canDetectFlower(String flowerId) {
            return flowers.contains(flowerId);
        }
    }
}
