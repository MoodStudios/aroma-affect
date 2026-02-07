package com.ovrtechnology;

import com.ovrtechnology.ability.AbilityDefinitionLoader;
import com.ovrtechnology.ability.AbilityHandler;
import com.ovrtechnology.ability.AbilityRegistry;
import com.ovrtechnology.ability.PreciseSnifferAbility;
import com.ovrtechnology.biome.BiomeDefinitionLoader;
import com.ovrtechnology.block.BlockDefinitionLoader;
import com.ovrtechnology.command.AromaTestCommand;
import com.ovrtechnology.flower.FlowerDefinitionLoader;
import com.ovrtechnology.structure.StructureDefinitionLoader;
import com.ovrtechnology.guide.AromaGuideFirstJoinHandler;
import com.ovrtechnology.guide.AromaGuideRegistry;
import com.ovrtechnology.command.path.ActivePathManager;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.mob.MobDefinitionLoader;
import com.ovrtechnology.network.NoseRenderNetworking;
import com.ovrtechnology.network.NoseSmithDialogueNetworking;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.network.SnifferEquipmentNetworking;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.omara.OmaraDeviceRegistry;
import com.ovrtechnology.registry.ModCreativeTab;
import com.ovrtechnology.registry.ModSounds;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.scentitem.ScentItemRegistry;
import com.ovrtechnology.sniffer.SnifferMenuRegistry;
import com.ovrtechnology.sniffernose.SnifferNoseRegistry;
import com.ovrtechnology.trigger.ScentTriggerManager;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.worldgen.VillagePoolInjector;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Aroma Affect mod.
 * This mod integrates OVR's scent hardware into Minecraft through the "Nose"
 * system.
 */
@UtilityClass
public final class AromaAffect {
    public static final String MOD_ID = "aromaaffect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Initializing Aroma Affect...");

        // Common networking (C2S/S2C packets)
        NoseSmithDialogueNetworking.init();
        PathScentNetworking.init();
        SnifferEquipmentNetworking.init();
        NoseRenderNetworking.init();

        // Load ability definitions first (needed for nose validation)
        AbilityDefinitionLoader.loadAllAbilities();

        // Initialize the nose registry system (includes ability resolver)
        // Note: This validates ability references against loaded definitions
        NoseRegistry.init();

        // Initialize the sniffer nose registry system (items for Sniffer mob)
        SnifferNoseRegistry.init();

        // Initialize scent definitions (needed for particle colors, display names, etc.)
        ScentRegistry.init();

        // Initialize the scent item registry system
        ScentItemRegistry.init();

        // Initialize the Aroma Guide (village compass)
        AromaGuideRegistry.init();

        // Initialize the Nose Smith entity registry
        NoseSmithRegistry.init();

        // Initialize Sniffer menu registry
        SnifferMenuRegistry.init();

        // Initialize Omara Device block
        OmaraDeviceRegistry.init();

        // Initialize custom sounds
        ModSounds.init();

        // Initialize creative tab
        ModCreativeTab.init();

        // Initialize lookup system
        LookupManager.init();

        // Initialize active path manager for persistent particle paths
        ActivePathManager.init();

        // Initialize ability system
        // Register ability implementations and initialize handler
        AbilityRegistry.register(PreciseSnifferAbility.INSTANCE);
        AbilityRegistry.init();
        AbilityHandler.init();

        // Load definition files (needed by trigger system)
        BiomeDefinitionLoader.loadAllBiomes();
        BlockDefinitionLoader.loadAllBlocks();
        FlowerDefinitionLoader.loadAllFlowers();
        StructureDefinitionLoader.loadAllStructures();
        MobDefinitionLoader.loadAllMobs();

        // Initialize scent trigger system
        ScentTriggerConfigLoader.init();
        ScentTriggerManager.init();

        // Initialize commands
        AromaTestCommand.init();

        // Give Aroma Guide on first join
        AromaGuideFirstJoinHandler.init();

        // Inject custom pieces into vanilla worldgen (villages, etc.)
        VillagePoolInjector.init();

        LOGGER.info("Aroma Affect initialized successfully!");
    }
}
