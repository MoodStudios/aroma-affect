package com.ovrtechnology;

import com.ovrtechnology.ability.AbilityHandler;
import com.ovrtechnology.command.AromaTestCommand;
import com.ovrtechnology.command.path.ActivePathManager;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.registry.ModCreativeTab;
import com.ovrtechnology.scentitem.ScentItemRegistry;
import com.ovrtechnology.sniffernose.SnifferNoseRegistry;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the AromaCraft mod.
 * This mod integrates OVR's scent hardware into Minecraft through the "Nose"
 * system.
 */
@UtilityClass
public final class AromaCraft {
    public static final String MOD_ID = "aromacraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Initializing AromaCraft...");

        // Initialize the nose registry system (includes ability resolver)
        NoseRegistry.init();

        // Initialize the sniffer nose registry system (items for Sniffer mob)
        SnifferNoseRegistry.init();

        // Initialize the scent item registry system
        ScentItemRegistry.init();

        // Initialize creative tab
        ModCreativeTab.init();

        // Initialize lookup system
        LookupManager.init();

        // Initialize active path manager for persistent particle paths
        ActivePathManager.init();

        // Initialize ability system (Precise Sniffer, etc.)
        AbilityHandler.init();

        // Initialize test commands
        AromaTestCommand.init();

        LOGGER.info("AromaCraft initialized successfully!");
    }
}
