package com.ovrtechnology.registry;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.biome.BiomeDefinition;
import com.ovrtechnology.biome.BiomeRegistry;
import com.ovrtechnology.block.BlockDefinition;
import com.ovrtechnology.block.BlockRegistry;
import com.ovrtechnology.structure.StructureDefinition;
import com.ovrtechnology.structure.StructureRegistry;
import com.ovrtechnology.util.Ids;
import dev.architectury.event.events.common.LifecycleEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class RegistryValidator {

    @Getter private static final Set<String> invalidStructureIds = new HashSet<>();

    @Getter private static final Set<String> invalidBiomeIds = new HashSet<>();

    @Getter private static final Set<String> invalidBlockIds = new HashSet<>();

    @Getter private static boolean validated = false;

    @Getter private static int errorCount = 0;

    private RegistryValidator() {
        throw new UnsupportedOperationException("RegistryValidator is a static utility class");
    }

    public static void init() {
        LifecycleEvent.SERVER_STARTED.register(RegistryValidator::onServerStarted);
        AromaAffect.LOGGER.info("RegistryValidator initialized - will validate on server start");
    }

    private static void onServerStarted(MinecraftServer server) {
        AromaAffect.LOGGER.info(
                "Validating Aroma Affect registries against Minecraft registries...");

        invalidStructureIds.clear();
        invalidBiomeIds.clear();
        invalidBlockIds.clear();
        errorCount = 0;
        validated = false;

        long startTime = System.currentTimeMillis();

        var level = server.overworld();
        var registryAccess = level.registryAccess();

        validateBlocks();

        validateBiomes(registryAccess.registryOrThrow(Registries.BIOME));

        validateStructures(registryAccess.registryOrThrow(Registries.STRUCTURE));

        validated = true;
        long elapsed = System.currentTimeMillis() - startTime;

        if (errorCount == 0) {
            AromaAffect.LOGGER.info(
                    "Registry validation complete in {}ms - all entries valid!", elapsed);
        } else {
            AromaAffect.LOGGER.warn(
                    "Registry validation complete in {}ms - found {} invalid entries",
                    elapsed,
                    errorCount);
            logValidationSummary();
        }
    }

    private static void validateBlocks() {
        if (!BlockRegistry.isInitialized()) {
            AromaAffect.LOGGER.warn("BlockRegistry not initialized, skipping block validation");
            return;
        }

        Registry<Block> blockRegistry = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        int checked = 0;
        int invalid = 0;

        for (BlockDefinition block : BlockRegistry.getAllBlocks()) {
            checked++;
            String blockId = block.getBlockId();
            ResourceLocation resourceLocation = Ids.tryParse(blockId);

            if (resourceLocation == null) {
                AromaAffect.LOGGER.error(
                        "[BlockRegistry] Invalid ResourceLocation format: '{}'", blockId);
                invalidBlockIds.add(blockId);
                invalid++;
                continue;
            }

            if (!blockRegistry.containsKey(resourceLocation)) {
                AromaAffect.LOGGER.warn(
                        "[BlockRegistry] Block not found in Minecraft registry: '{}' - "
                                + "this block will not be trackable",
                        blockId);
                invalidBlockIds.add(blockId);
                invalid++;
            }
        }

        errorCount += invalid;
        AromaAffect.LOGGER.info(
                "Validated {} blocks: {} valid, {} invalid", checked, checked - invalid, invalid);
    }

    private static void validateBiomes(Registry<Biome> biomeRegistry) {
        if (!BiomeRegistry.isInitialized()) {
            AromaAffect.LOGGER.warn("BiomeRegistry not initialized, skipping biome validation");
            return;
        }

        int checked = 0;
        int invalid = 0;

        for (BiomeDefinition biome : BiomeRegistry.getAllBiomes()) {
            checked++;
            String biomeId = biome.getBiomeId();
            ResourceLocation resourceLocation = Ids.tryParse(biomeId);

            if (resourceLocation == null) {
                AromaAffect.LOGGER.error(
                        "[BiomeRegistry] Invalid ResourceLocation format: '{}'", biomeId);
                invalidBiomeIds.add(biomeId);
                invalid++;
                continue;
            }

            if (!biomeRegistry.containsKey(resourceLocation)) {

                String namespace = biome.getNamespace();
                if ("minecraft".equals(namespace)) {
                    AromaAffect.LOGGER.error(
                            "[BiomeRegistry] Vanilla biome not found: '{}' - "
                                    + "this may indicate a typo or version mismatch",
                            biomeId);
                } else {
                    AromaAffect.LOGGER.warn(
                            "[BiomeRegistry] Modded biome not found: '{}' - "
                                    + "the mod '{}' may not be installed or the biome ID may be incorrect",
                            biomeId,
                            namespace);
                }
                invalidBiomeIds.add(biomeId);
                invalid++;
            }
        }

        errorCount += invalid;
        AromaAffect.LOGGER.info(
                "Validated {} biomes: {} valid, {} invalid", checked, checked - invalid, invalid);
    }

    private static void validateStructures(Registry<Structure> structureRegistry) {
        if (!StructureRegistry.isInitialized()) {
            AromaAffect.LOGGER.warn(
                    "StructureRegistry not initialized, skipping structure validation");
            return;
        }

        int checked = 0;
        int invalid = 0;

        AromaAffect.LOGGER.debug(
                "Available structures in world: {}",
                structureRegistry.keySet().stream().map(ResourceLocation::toString).toList());

        for (StructureDefinition structure : StructureRegistry.getAllStructures()) {
            checked++;
            String structureId = structure.getStructureId();
            ResourceLocation resourceLocation = Ids.tryParse(structureId);

            if (resourceLocation == null) {
                AromaAffect.LOGGER.error(
                        "[StructureRegistry] Invalid ResourceLocation format: '{}'", structureId);
                invalidStructureIds.add(structureId);
                invalid++;
                continue;
            }

            if (!structureRegistry.containsKey(resourceLocation)) {

                String namespace = structure.getNamespace();
                if ("minecraft".equals(namespace)) {
                    AromaAffect.LOGGER.error(
                            "[StructureRegistry] Vanilla structure not found: '{}' - "
                                    + "this may indicate a typo or version mismatch",
                            structureId);
                } else {
                    AromaAffect.LOGGER.warn(
                            "[StructureRegistry] Modded structure not found: '{}' - "
                                    + "the mod '{}' may not be installed or the structure ID may be incorrect",
                            structureId,
                            namespace);
                }
                invalidStructureIds.add(structureId);
                invalid++;
            }
        }

        errorCount += invalid;
        AromaAffect.LOGGER.info(
                "Validated {} structures: {} valid, {} invalid",
                checked,
                checked - invalid,
                invalid);
    }

    private static void logValidationSummary() {
        AromaAffect.LOGGER.warn("=== Registry Validation Summary ===");

        if (!invalidBlockIds.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "Invalid blocks ({}): {}", invalidBlockIds.size(), invalidBlockIds);
        }

        if (!invalidBiomeIds.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "Invalid biomes ({}): {}", invalidBiomeIds.size(), invalidBiomeIds);
        }

        if (!invalidStructureIds.isEmpty()) {
            AromaAffect.LOGGER.warn(
                    "Invalid structures ({}): {}", invalidStructureIds.size(), invalidStructureIds);
        }

        AromaAffect.LOGGER.warn(
                "These entries will not be trackable. Check your JSON configuration files.");
        AromaAffect.LOGGER.warn("===================================");
    }

    public static List<BlockDefinition> getValidBlocks() {
        if (!validated || invalidBlockIds.isEmpty()) {
            return BlockRegistry.getAllBlocksAsList();
        }

        List<BlockDefinition> valid = new ArrayList<>();
        for (BlockDefinition block : BlockRegistry.getAllBlocks()) {
            if (!invalidBlockIds.contains(block.getBlockId())) {
                valid.add(block);
            }
        }
        return valid;
    }

    static void reset() {
        invalidStructureIds.clear();
        invalidBiomeIds.clear();
        invalidBlockIds.clear();
        errorCount = 0;
        validated = false;
    }
}
