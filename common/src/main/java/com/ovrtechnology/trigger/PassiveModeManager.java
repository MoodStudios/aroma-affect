package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaCraft;
import com.ovrtechnology.search.SearchManager;
import com.ovrtechnology.trigger.config.BiomeTriggerDefinition;
import com.ovrtechnology.trigger.config.BlockTriggerDefinition;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.StructureTriggerDefinition;
import com.ovrtechnology.websocket.OvrWebSocketClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages the passive-mode scent system.
 * 
 * <p>Passive-mode automatically emits scents when the player is near predefined
 * blocks, biomes, or structures, but ONLY when:</p>
 * <ul>
 *   <li>OVR hardware is connected</li>
 *   <li>Player does NOT have a nose equipped</li>
 * </ul>
 * 
 * <p>This uses an optimized tick-based detection system with caching to minimize
 * performance impact.</p>
 */
public final class PassiveModeManager {

    /**
     * Development mode flag - bypasses OVR hardware check.
     * Set to false for production builds.
     */
    private static final boolean DEV_MODE = true;

    /**
     * How often to check for passive triggers (in ticks).
     * Checking every 300 ticks (15 seconds)
     * without excessive performance overhead.
     */
    private static final int CHECK_INTERVAL_TICKS = 300;
    
    /**
     * Cache for the last detected block/biome/structure to avoid redundant triggers.
     */
    private static final Map<String, BlockPos> lastDetectedBlock = new HashMap<>();
    private static String lastDetectedBiome = null;
    private static String lastDetectedStructure = null;
    private static int tickCounter = 0;
    
    private PassiveModeManager() {
    }
    
    /**
     * Processes one game tick for passive-mode detection.
     * Should be called every client tick from ScentTriggerHandler.
     * 
     * @param player the player to check
     */
    public static void tick(Player player) {
        if (player == null) {
            AromaCraft.LOGGER.warn("Passive-mode tick called with null player!");
            return;
        }
        
        // Only check every N ticks for performance
        if (++tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        
        // Log every CHECK_INTERVAL_TICKS to confirm tick is working
        AromaCraft.LOGGER.debug("[PassiveModeManager] Processing tick (every {} ticks)", CHECK_INTERVAL_TICKS);
        
        // CRITICAL CHECKS - Early returns for performance
        
        // 1. Check if OVR hardware is connected (skip in DEV_MODE)
        if (!DEV_MODE && !OvrWebSocketClient.getInstance().isConnected()) {
            // Hardware not connected - passive mode disabled
            AromaCraft.LOGGER.debug("Passive-mode disabled: OVR hardware not connected");
            clearPassiveScent();
            return;
        }
        
        // 2. Check if player has nose equipped
        if (SearchManager.isNoseEquipped(player)) {
            // Player has nose - passive mode disabled
            AromaCraft.LOGGER.debug("Passive-mode disabled: Nose equipped");
            clearPassiveScent();
            return;
        }
        
        // All conditions met - check for passive triggers
        Level level = player.level();
        if (!level.isClientSide()) {
            return;
        }
        
        BlockPos playerPos = player.blockPosition();
        
        // Check block proximity triggers
        checkBlockProximity(player, level, playerPos);
        
        // Check biome triggers
        checkBiomeProximity(player, level, playerPos);
        
        // Check structure proximity triggers
        checkStructureProximity(player, level, playerPos);
    }
    
    /**
     * Checks for nearby blocks that should trigger passive scents.
     */
    private static void checkBlockProximity(Player player, Level level, BlockPos playerPos) {
        // Get all block triggers from configuration
        // Note: getAllBlockTriggers() needs to be added to ScentTriggerConfigLoader
        
        // For now, iterate through configured block triggers
        // We'll need to add a method to get all block triggers
        for (BlockTriggerDefinition trigger : ScentTriggerConfigLoader.getAllBlockTriggers()) {
            if (!trigger.isProximityTrigger() || !trigger.isValid()) {
                continue;
            }
            
            String blockId = trigger.getBlockId();
            int range = trigger.getRange();
            
            // Check if we already detected this block nearby
            BlockPos lastPos = lastDetectedBlock.get(blockId);
            if (lastPos != null && playerPos.distSqr(lastPos) < (range * range)) {
                // Still near the same block - keep the trigger active
                continue;
            }
            
            // Search for nearby blocks
            Optional<BlockPos> foundPos = findNearbyBlock(level, playerPos, blockId, range);
            
            if (foundPos.isPresent()) {
                // Found a trigger block nearby
                BlockPos blockPos = foundPos.get();
                
                // Only trigger if this is a new detection (not already cached)
                if (!lastDetectedBlock.containsKey(blockId)) {
                    lastDetectedBlock.put(blockId, blockPos);
                    
                    // Create and trigger scent
                    ScentTrigger scentTrigger = ScentTrigger.create(
                        trigger.getScentName(),
                        ScentTriggerSource.PASSIVE_MODE,
                        trigger.getPriority(),
                        -1 // Indefinite - will last as long as player is nearby
                    );
                    
                    ScentTriggerManager.getInstance().trigger(scentTrigger);
                    
                    // Send chat message to player
                    String blockName = getBlockDisplayName(level, blockId);
                    player.displayClientMessage(
                        Component.literal("§6[AromaCraft] §7Detected Scent: §e" + trigger.getScentName() +
                            " §7 near block §b" + blockName),
                        false
                    );
                    
                    AromaCraft.LOGGER.debug("Passive-mode triggered: {} near block {}", 
                        trigger.getScentName(), blockId);
                } else {
                    // Update position in cache
                    lastDetectedBlock.put(blockId, blockPos);
                }
            } else {
                // No longer near this block - remove from cache
                lastDetectedBlock.remove(blockId);
            }
        }
    }
    
    /**
     * Checks for current biome that should trigger passive scents.
     */
    private static void checkBiomeProximity(Player player, Level level, BlockPos playerPos) {
        // Get biome at player position
        var biomeHolder = level.getBiome(playerPos);
        String biomeId = Objects.requireNonNull(level.registryAccess().lookupOrThrow(Registries.BIOME)
                .getKey(biomeHolder.value())).toString();
        
        // Check if biome changed
        if (biomeId.equals(lastDetectedBiome)) {
            return; // Same biome - no change needed
        }
        
        // Biome changed - check for triggers
        Optional<BiomeTriggerDefinition> triggerOpt = 
            ScentTriggerConfigLoader.getBiomeTrigger(biomeId);
        
        if (triggerOpt.isPresent()) {
            BiomeTriggerDefinition trigger = triggerOpt.get();
            
            // Only trigger if biome changed
            boolean biomeChanged = !biomeId.equals(lastDetectedBiome);
            
            if (trigger.isAmbient()) {
                // Continuous ambient scent while in biome
                if (biomeChanged) {
                    ScentTrigger scentTrigger = ScentTrigger.create(
                        trigger.getScentName(),
                        ScentTriggerSource.PASSIVE_MODE,
                        trigger.getPriority(),
                        -1 // Indefinite - lasts while in biome
                    );
                    
                    ScentTriggerManager.getInstance().trigger(scentTrigger);
                    lastDetectedBiome = biomeId;
                    
                    // Send chat message to player
                    String biomeName = getBiomeDisplayName(biomeId);
                    player.displayClientMessage(
                        Component.literal("§6[AromaCraft] §7Detected Scent: §e" + trigger.getScentName() +
                            " §7near biome §b" + biomeName),
                        false
                    );
                    
                    AromaCraft.LOGGER.debug("Passive-mode triggered: {} from biome {}", 
                        trigger.getScentName(), biomeId);
                }
            } else if (trigger.isEnterTrigger()) {
                // One-time trigger on entering biome
                if (biomeChanged) {
                    ScentTrigger scentTrigger = ScentTrigger.create(
                        trigger.getScentName(),
                        ScentTriggerSource.PASSIVE_MODE,
                        trigger.getPriority(),
                        100 // Short duration for entry scent
                    );
                    
                    ScentTriggerManager.getInstance().trigger(scentTrigger);
                    lastDetectedBiome = biomeId;
                    
                    // Send chat message to player
                    String biomeName = getBiomeDisplayName(biomeId);
                    player.displayClientMessage(
                        Component.literal("§6[AromaCraft] §7detected scent: §e" + trigger.getScentName() +
                            " §7al enter the biome §b" + biomeName),
                        false
                    );
                    
                    AromaCraft.LOGGER.debug("Passive-mode triggered: {} entering biome {}", 
                        trigger.getScentName(), biomeId);
                }
            }
        } else {
            // No trigger for this biome - clear last detected
            if (lastDetectedBiome != null) {
                lastDetectedBiome = null;
                // Stop any passive-mode scents if they exist
                // The active scent will be replaced if a new one triggers
            }
        }
    }
    
    /**
     * Checks for nearby structures that should trigger passive scents.
     * 
     * <p>Structure detection works by checking if the player is within range
     * of any configured structure based on their current position.</p>
     */
    private static void checkStructureProximity(Player player, Level level, BlockPos playerPos) {
        // Structure detection requires server-side access
        // On client, we need to use integrated server or skip
        ServerLevel serverLevel = getServerLevel(level);
        if (serverLevel == null) {
            return;
        }
        
        String foundStructureId = null;
        StructureTriggerDefinition foundTrigger = null;
        
        // Check each configured structure trigger
        for (StructureTriggerDefinition trigger : ScentTriggerConfigLoader.getAllStructureTriggers()) {
            if (!trigger.isProximityTrigger() || !trigger.isValid()) {
                continue;
            }
            
            String structureId = trigger.getStructureId();
            int range = trigger.getRange();
            
            // Check if player is near this structure type
            if (isNearStructure(serverLevel, playerPos, structureId, range)) {
                foundStructureId = structureId;
                foundTrigger = trigger;
                break; // Found a structure, stop searching
            }
        }
        
        // Handle structure detection changes
        if (foundStructureId != null && foundTrigger != null) {
            // Found a structure nearby
            if (!foundStructureId.equals(lastDetectedStructure)) {
                // New structure detected
                lastDetectedStructure = foundStructureId;
                
                // Create and trigger scent
                ScentTrigger scentTrigger = ScentTrigger.create(
                    foundTrigger.getScentName(),
                    ScentTriggerSource.PASSIVE_MODE,
                    foundTrigger.getPriority(),
                    -1 // Indefinite - lasts while near structure
                );
                
                ScentTriggerManager.getInstance().trigger(scentTrigger);
                
                // Send chat message to player
                String structureName = formatResourceId(foundStructureId);
                player.displayClientMessage(
                    Component.literal("§6[AromaCraft] §7Detected Scent: §e" + foundTrigger.getScentName() +
                        " §7near structure §b" + structureName),
                    false
                );
                
                AromaCraft.LOGGER.debug("Passive-mode triggered: {} near structure {}", 
                    foundTrigger.getScentName(), foundStructureId);
            }
        } else {
            // No structure nearby - clear last detected
            if (lastDetectedStructure != null) {
                lastDetectedStructure = null;
            }
        }
    }
    
    /**
     * Gets the ServerLevel from a Level (works for integrated server).
     * 
     * @param level the level to convert
     * @return ServerLevel if available, null otherwise
     */
    private static ServerLevel getServerLevel(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel;
        }
        
        // Try to get from integrated server (single player)
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            return minecraft.getSingleplayerServer().getLevel(level.dimension());
        }
        
        return null;
    }
    
    /**
     * Checks if the player is near a specific structure type.
     * 
     * @param level the server level
     * @param playerPos player position
     * @param structureId the structure ID to check for
     * @param range detection range in blocks
     * @return true if player is within range of the structure
     */
    private static boolean isNearStructure(ServerLevel level, BlockPos playerPos, String structureId, int range) {
        try {
            ResourceLocation structureLocation = ResourceLocation.parse(structureId);
            
            // Get the structure from registry
            var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            var structureOpt = structureRegistry.getOptional(structureLocation);
            
            if (structureOpt.isEmpty()) {
                return false;
            }
            
            Structure structure = structureOpt.get();
            
            // Check chunks within range for structure starts
            int chunkRange = (range / 16) + 1;
            SectionPos playerSection = SectionPos.of(playerPos);
            
            for (int cx = -chunkRange; cx <= chunkRange; cx++) {
                for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                    int chunkX = playerSection.x() + cx;
                    int chunkZ = playerSection.z() + cz;
                    
                    // Get structure start at this chunk
                    StructureStart structureStart = level.structureManager()
                        .getStartForStructure(
                            SectionPos.of(chunkX, 0, chunkZ),
                            structure,
                            level.getChunk(chunkX, chunkZ)
                        );
                    
                    if (structureStart != null && structureStart.isValid()) {
                        // Check if player is within range of structure bounding box
                        var boundingBox = structureStart.getBoundingBox();
                        
                        // Calculate distance to bounding box
                        int distX = Math.max(0, Math.max(boundingBox.minX() - playerPos.getX(), playerPos.getX() - boundingBox.maxX()));
                        int distY = Math.max(0, Math.max(boundingBox.minY() - playerPos.getY(), playerPos.getY() - boundingBox.maxY()));
                        int distZ = Math.max(0, Math.max(boundingBox.minZ() - playerPos.getZ(), playerPos.getZ() - boundingBox.maxZ()));
                        
                        double distance = Math.sqrt(distX * distX + distY * distY + distZ * distZ);
                        
                        if (distance <= range) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.debug("Error checking structure {}: {}", structureId, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Finds a nearby block of the specified type within range.
     * 
     * @param level the level to search in
     * @param center the center position to search from
     * @param blockId the block ID to search for (e.g., "minecraft:campfire")
     * @param range the search range in blocks
     * @return the position of a found block, or empty if none found
     */
    private static Optional<BlockPos> findNearbyBlock(Level level, BlockPos center, 
                                                      String blockId, int range) {
        try {
            ResourceLocation blockLocation = ResourceLocation.parse(blockId);
            Optional<Block> blockOpt = level.registryAccess()
                .lookupOrThrow(Registries.BLOCK)
                .getOptional(blockLocation);
            
            if (blockOpt.isEmpty()) {
                return Optional.empty();
            }
            
            Block targetBlock = blockOpt.get();
            
            // Search in a cubic area around the player
            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        BlockPos checkPos = center.offset(x, y, z);
                        BlockState state = level.getBlockState(checkPos);
                        
                        if (state.is(targetBlock)) {
                            return Optional.of(checkPos);
                        }
                    }
                }
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.warn("Error searching for block {}: {}", blockId, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets a display name for a block ID.
     * Formats the resource location into a readable name.
     * 
     * @param level the level (for registry access)
     * @param blockId the block ID (e.g., "minecraft:campfire")
     * @return a formatted display name
     */
    private static String getBlockDisplayName(Level level, String blockId) {
        try {
            ResourceLocation location = ResourceLocation.parse(blockId);
            Optional<Block> blockOpt = level.registryAccess()
                .lookupOrThrow(Registries.BLOCK)
                .getOptional(location);
            
            if (blockOpt.isPresent()) {
                Block block = blockOpt.get();
                // Use the block's display name if available, otherwise use the ID
                String displayName = block.getDescriptionId();
                // Remove "block." prefix and format
                if (displayName.startsWith("block.")) {
                    displayName = displayName.substring(6);
                }
                // Capitalize first letter and replace underscores with spaces
                displayName = formatResourceName(displayName);
                return displayName;
            }
        } catch (Exception e) {
            AromaCraft.LOGGER.debug("Error getting block display name for {}: {}", blockId, e.getMessage());
        }
        
        // Fallback: format the resource ID
        return formatResourceId(blockId);
    }
    
    /**
     * Gets a display name for a biome ID.
     * Formats the resource location into a readable name.
     * 
     * @param biomeId the biome ID (e.g., "minecraft:forest")
     * @return a formatted display name
     */
    private static String getBiomeDisplayName(String biomeId) {
        // Format the resource ID
        return formatResourceId(biomeId);
    }
    
    /**
     * Formats a resource ID into a readable display name.
     * Example: "minecraft:forest" -> "Forest"
     * 
     * @param resourceId the resource ID
     * @return formatted name
     */
    private static String formatResourceId(String resourceId) {
        try {
            ResourceLocation location = ResourceLocation.parse(resourceId);
            String path = location.getPath();
            // Replace underscores with spaces and capitalize
            return formatResourceName(path);
        } catch (Exception e) {
            // Fallback: just return the ID
            return resourceId;
        }
    }
    
    /**
     * Formats a resource name by replacing underscores with spaces and capitalizing words.
     * 
     * @param name the resource name (e.g., "campfire" or "forest_biome")
     * @return formatted name (e.g., "Campfire" or "Forest Biome")
     */
    private static String formatResourceName(String name) {
        // Replace underscores with spaces
        String formatted = name.replace('_', ' ');
        // Split by spaces and capitalize each word
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }
        return result.toString();
    }
    
    /**
     * Clears any active passive-mode scents.
     * Called when conditions are no longer met (no hardware, nose equipped, etc.).
     */
    private static void clearPassiveScent() {
        ScentTriggerManager manager = ScentTriggerManager.getInstance();
        manager.getActiveScentOptional()
            .filter(trigger -> trigger.source() == ScentTriggerSource.PASSIVE_MODE)
            .ifPresent(trigger -> {
                manager.stop();
                AromaCraft.LOGGER.debug("Passive-mode scent stopped: {}", trigger.scentName());
            });
        
        // Clear caches
        lastDetectedBlock.clear();
        lastDetectedBiome = null;
        lastDetectedStructure = null;
    }
    
    /**
     * Stops passive-mode for a specific player.
     * Called externally when needed (e.g., player disconnects).
     */
    public static void stopPassiveMode() {
        clearPassiveScent();
    }
}
