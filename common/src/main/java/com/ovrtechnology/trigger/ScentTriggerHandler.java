package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaAffect;
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
 *   <li>Passive-mode detection (proximity-based triggers when hardware connected and no nose equipped)</li>
 * </ul>
 * 
 * <h3>Future Implementations (Placeholders):</h3>
 * <ul>
 *   <li>Biome change detection</li>
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
            AromaAffect.LOGGER.warn("ScentTriggerHandler.init() called multiple times!");
            return;
        }
        
        AromaAffect.LOGGER.info("[ScentTriggerHandler] Starting initialization...");
        
        try {
            // Register client tick handler for processing scent durations and passive-mode
            ClientTickEvent.CLIENT_POST.register(minecraft -> {
                // Process active scent durations
                ScentTriggerManager.getInstance().tick();
                
                // Process passive-mode detection (only if player exists)
                if (minecraft.player != null) {
                    PassiveModeManager.tick(minecraft.player);
                }

                // Check for delayed all-scents victory
                com.ovrtechnology.network.TutorialScentCounterNetworking.tickClient();
            });
            
            AromaAffect.LOGGER.info("[ScentTriggerHandler] ClientTickEvent.CLIENT_POST registered successfully!");
        } catch (Exception e) {
            AromaAffect.LOGGER.error("[ScentTriggerHandler] Failed to register ClientTickEvent!", e);
        }
        
        initialized = true;
        AromaAffect.LOGGER.info("[ScentTriggerHandler] Initialization complete");
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
