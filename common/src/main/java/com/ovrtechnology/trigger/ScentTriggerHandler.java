package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaCraft;
import dev.architectury.event.events.client.ClientTickEvent;

/**
 * Handles game events that can trigger scents.
 * 
 * <p>This class registers event listeners using Architectury's cross-platform
 * event system and delegates to the appropriate trigger logic.</p>
 * 
 * <h3>Currently Implemented:</h3>
 * <ul>
 *   <li>Client tick processing for scent durations</li>
 * </ul>
 * 
 * <h3>Future Implementations (Placeholders):</h3>
 * <ul>
 *   <li>Biome change detection</li>
 *   <li>Block proximity detection</li>
 *   <li>Mob proximity detection</li>
 * </ul>
 */
public final class ScentTriggerHandler {
    
    /**
     * Whether the handler has been initialized.
     */
    private static boolean initialized = false;
    
    private ScentTriggerHandler() {
    }
    
    /**
     * Initializes the trigger handler and registers event listeners.
     * Should be called during client-side mod initialization.
     */
    public static void init() {
        if (initialized) {
            AromaCraft.LOGGER.warn("ScentTriggerHandler.init() called multiple times!");
            return;
        }
        
        AromaCraft.LOGGER.info("Initializing ScentTriggerHandler...");
        
        // Register client tick handler for processing scent durations
        ClientTickEvent.CLIENT_POST.register(instance -> {
            ScentTriggerManager.getInstance().tick();
        });
        
        // Future: Register biome change listener
        // BiomeChangeEvent.register(ScentTriggerHandler::onBiomeChange);
        
        // Future: Register proximity check on tick
        // Can be added here when implementing block/mob proximity triggers
        
        initialized = true;
        AromaCraft.LOGGER.info("ScentTriggerHandler initialized");
    }
    
    // ========================================
    // Future Event Handlers (Placeholders)
    // ========================================
    
    /*
     * Future: Biome change handler
     * 
     * private static void onBiomeChange(Player player, Biome oldBiome, Biome newBiome) {
     *     String biomeId = newBiome.getRegistryName().toString();
     *     ScentTriggerConfigLoader.getBiomeTrigger(biomeId).ifPresent(trigger -> {
     *         if (trigger.isEnterTrigger()) {
     *             ScentTrigger scentTrigger = ScentTrigger.create(
     *                 trigger.getScentName(),
     *                 ScentTriggerSource.BIOME_ENTER,
     *                 trigger.getPriority(),
     *                 100 // Short duration for entry scent
     *             );
     *             ScentTriggerManager.getInstance().trigger(scentTrigger);
     *         }
     *     });
     * }
     */
    
    /*
     * Future: Block proximity check (called each tick or periodically)
     * 
     * private static void checkBlockProximity(Player player) {
     *     BlockPos playerPos = player.blockPosition();
     *     // Scan nearby blocks for triggers
     *     // Use BlockTriggerDefinition.getRange() for search radius
     * }
     */
    
    /*
     * Future: Mob proximity check (called each tick or periodically)
     * 
     * private static void checkMobProximity(Player player) {
     *     List<Entity> nearbyEntities = player.level().getEntitiesOfClass(
     *         LivingEntity.class,
     *         player.getBoundingBox().inflate(5)
     *     );
     *     // Check each entity type against mob triggers
     * }
     */
    
    /**
     * Checks if the handler has been initialized.
     * 
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
