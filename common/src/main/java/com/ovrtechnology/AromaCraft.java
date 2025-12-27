package com.ovrtechnology;

import com.ovrtechnology.biome.BiomeRegistry;
import com.ovrtechnology.block.BlockRegistry;
import com.ovrtechnology.command.AromaTestCommand;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.registry.ModCreativeTab;
import com.ovrtechnology.registry.RegistryValidator;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.structure.StructureRegistry;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the AromaCraft mod.
 * This mod integrates OVR's scent hardware into Minecraft through the "Nose" system.
 */
@UtilityClass
public final class AromaCraft {
    public static final String MOD_ID = "aromacraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Initializing AromaCraft...");
        
        // ============================================================
        // PHASE 1: Load JSON definitions and validate internal references
        // ============================================================
        // This happens during mod initialization. At this point we CAN'T
        // validate against Minecraft's registries because:
        // - Dynamic registries (structures, biomes) are not populated yet
        // - Other mods haven't finished registering their content
        //
        // Initialization order (dependency chain):
        // 1. Scents (no dependencies)
        // 2. Blocks (validates scent references)
        // 3. Biomes (validates scent references)  
        // 4. Structures (validates scent references)
        // 5. Noses (validates block, biome, and structure references, registers items)
        
        // Initialize the scent registry system
        ScentRegistry.init();
        
        // Initialize the block registry system (validates scent_id references)
        BlockRegistry.init();
        
        // Initialize the biome registry system (validates scent_id references)
        BiomeRegistry.init();
        
        // Initialize the structure registry system (validates scent_id references)
        StructureRegistry.init();
        
        // Initialize the nose registry system (validates block/biome/structure references, includes ability resolver)
        NoseRegistry.init();
        
        // Initialize creative tab
        ModCreativeTab.init();
        
        // Initialize lookup system
        LookupManager.init();
        
        // ============================================================
        // PHASE 2: Register post-load validation (runs on SERVER_STARTED)
        // ============================================================
        // Once the server starts and the world is loaded, we validate that
        // all configured blocks/biomes/structures actually exist in Minecraft's
        // registries. This catches:
        // - Typos in IDs (e.g., "minecraft:stronghod" instead of "stronghold")
        // - Modded content from mods that aren't installed
        // - Version mismatches (structure/biome renamed or removed)
        RegistryValidator.init();
        
        // Initialize test commands
        AromaTestCommand.init();
        
        LOGGER.info("AromaCraft initialized successfully!");
    }
}
