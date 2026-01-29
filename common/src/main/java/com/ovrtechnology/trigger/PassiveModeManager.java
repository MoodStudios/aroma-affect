package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.config.BiomeTriggerDefinition;
import com.ovrtechnology.trigger.config.BlockTriggerDefinition;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.StructureTriggerDefinition;
import com.ovrtechnology.trigger.config.TriggerSettings;
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

import java.util.Objects;
import java.util.Optional;

/**
 * Manages the passive-mode scent system with priority-based trigger selection.
 *
 * <p>Passive-mode automatically emits scents when the player is near predefined
 * blocks, biomes, or structures, but ONLY when:</p>
 * <ul>
 *   <li>OVR hardware is connected</li>
 *   <li>Player does NOT have a nose equipped</li>
 * </ul>
 *
 * <p><b>Priority System:</b></p>
 * <ul>
 *   <li>Blocks (MEDIUM) - highest priority, override everything</li>
 *   <li>Structures (MEDLOW) - override biomes</li>
 *   <li>Biomes (LOW) - base/ambient scent</li>
 * </ul>
 *
 * <p>Only ONE scent is active at a time. When the player leaves a high-priority
 * area, the system automatically falls back to the next available priority level.</p>
 */
public final class PassiveModeManager {

    /**
     * Development mode flag - bypasses OVR hardware check.
     * Set to false for production builds.
     */
    private static final boolean DEV_MODE = false;

    /**
     * How often to check for passive triggers (in ticks).
     * Checking every 300 ticks (15 seconds)
     */
    private static final int CHECK_INTERVAL_TICKS = 300;

    /**
     * Currently active passive trigger (for change detection).
     */
    private static ScentTrigger currentPassiveTrigger = null;

    /**
     * Source description for logging (e.g., "block:minecraft:lava").
     */
    private static String currentTriggerSource = null;

    /**
     * Type of the currently active trigger (for cooldown display).
     */
    private static TriggerType currentTriggerType = null;

    private static int tickCounter = 0;

    private PassiveModeManager() {
    }

    /**
     * Processes one game tick for passive-mode detection.
     * Evaluates all triggers and activates the highest priority one.
     *
     * @param player the player to check
     */
    public static void tick(Player player) {
        if (player == null) {
            AromaAffect.LOGGER.warn("Passive-mode tick called with null player!");
            return;
        }

        // Only check every N ticks for performance
        if (++tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        AromaAffect.LOGGER.debug("[PassiveModeManager] Processing tick (every {} ticks)", CHECK_INTERVAL_TICKS);

        // CRITICAL CHECKS - Early returns for performance

        // 1. Check if OVR hardware is connected (skip in DEV_MODE)
        if (!DEV_MODE && !OvrWebSocketClient.getInstance().isConnected()) {
            AromaAffect.LOGGER.debug("Passive-mode disabled: OVR hardware not connected");
            clearPassiveScent(player);
            return;
        }

        // Note: Passive-mode now works with or without nose equipped.
        // Previously it was disabled when nose was equipped, but users
        // want automatic scent detection regardless of nose status.

        // All conditions met - evaluate triggers by priority
        Level level = player.level();
        if (!level.isClientSide()) {
            return;
        }

        BlockPos playerPos = player.blockPosition();

        // Evaluate triggers in priority order (highest first)
        // Only the first match that passes cooldown check will be used

        // 1. Check blocks (MEDIUM priority - highest)
        TriggerCandidate blockCandidate = evaluateBlockTriggers(level, playerPos);
        if (blockCandidate != null && canActivateScent(blockCandidate)) {
            activateIfChanged(player, blockCandidate);
            return;
        }

        // 2. Check structures (MEDLOW priority)
        TriggerCandidate structureCandidate = evaluateStructureTriggers(level, playerPos);
        if (structureCandidate != null && canActivateScent(structureCandidate)) {
            activateIfChanged(player, structureCandidate);
            return;
        }

        // 3. Check biomes (LOW priority - lowest)
        TriggerCandidate biomeCandidate = evaluateBiomeTriggers(level, playerPos);
        if (biomeCandidate != null && canActivateScent(biomeCandidate)) {
            activateIfChanged(player, biomeCandidate);
            return;
        }

        // No valid triggers found (either none present or all on cooldown)
        if (currentPassiveTrigger != null) {
            AromaAffect.LOGGER.debug("No valid passive triggers found, clearing active scent");
            clearPassiveScent(player);
        }
    }

    /**
     * Types of passive triggers for cooldown selection.
     */
    private enum TriggerType {
        BLOCK,
        STRUCTURE,
        BIOME
    }

    /**
     * Represents a candidate trigger with its source information.
     */
    private record TriggerCandidate(
        ScentTrigger trigger,
        String source,
        String displayName,
        TriggerType type
    ) {}

    /**
     * Checks if a scent can be activated (respects per-type cooldown).
     * If the scent is already active from the same source, it's allowed.
     *
     * @param candidate the trigger candidate to check
     * @return true if the scent can be activated
     */
    private static boolean canActivateScent(TriggerCandidate candidate) {
        // If this is the same source as currently active, always allow (no change needed)
        if (currentTriggerSource != null && currentTriggerSource.equals(candidate.source)) {
            return true;
        }

        // Get the appropriate cooldown based on trigger type
        String scentName = candidate.trigger.scentName();
        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        long cooldownMs = getCooldownForType(candidate.type, settings);

        boolean canTrigger = ScentTriggerManager.getInstance().canTrigger(scentName, cooldownMs);

        if (!canTrigger) {
            AromaAffect.LOGGER.debug("Scent '{}' blocked by {} cooldown ({}ms)",
                scentName, candidate.type, cooldownMs);
        }

        return canTrigger;
    }

    /**
     * Gets the appropriate cooldown duration for a trigger type.
     */
    private static long getCooldownForType(TriggerType type, TriggerSettings settings) {
        return switch (type) {
            case BLOCK -> settings.getBlockCooldownMs();
            case STRUCTURE -> settings.getStructureCooldownMs();
            case BIOME -> settings.getBiomeCooldownMs();
        };
    }

    /**
     * Evaluates block triggers and returns the first match.
     */
    private static TriggerCandidate evaluateBlockTriggers(Level level, BlockPos playerPos) {
        for (BlockTriggerDefinition trigger : ScentTriggerConfigLoader.getAllBlockTriggers()) {
            if (!trigger.isProximityTrigger() || !trigger.isValid()) {
                continue;
            }

            String blockId = trigger.getBlockId();
            int range = trigger.getRange();

            Optional<BlockPos> foundPos = findNearbyBlock(level, playerPos, blockId, range);

            if (foundPos.isPresent()) {
                TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
                double intensity = trigger.getIntensityOrDefault(settings.getBlockIntensity());

                ScentTrigger scentTrigger = ScentTrigger.fromPassiveMode(
                    trigger.getScentName(),
                    trigger.getPriority(),
                    -1,
                    intensity
                );

                String source = "block:" + blockId;
                String displayName = getBlockDisplayName(level, blockId);

                return new TriggerCandidate(scentTrigger, source, displayName, TriggerType.BLOCK);
            }
        }
        return null;
    }

    /**
     * Evaluates structure triggers and returns the first match.
     */
    private static TriggerCandidate evaluateStructureTriggers(Level level, BlockPos playerPos) {
        ServerLevel serverLevel = getServerLevel(level);
        if (serverLevel == null) {
            return null;
        }

        for (StructureTriggerDefinition trigger : ScentTriggerConfigLoader.getAllStructureTriggers()) {
            if (!trigger.isProximityTrigger() || !trigger.isValid()) {
                continue;
            }

            String structureId = trigger.getStructureId();
            int range = trigger.getRange();

            if (isNearStructure(serverLevel, playerPos, structureId, range)) {
                TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
                double intensity = trigger.getIntensityOrDefault(settings.getStructureIntensity());

                ScentTrigger scentTrigger = ScentTrigger.fromPassiveMode(
                    trigger.getScentName(),
                    trigger.getPriority(),
                    -1,
                    intensity
                );

                String source = "structure:" + structureId;
                String displayName = formatResourceId(structureId);

                return new TriggerCandidate(scentTrigger, source, displayName, TriggerType.STRUCTURE);
            }
        }
        return null;
    }

    /**
     * Evaluates biome triggers and returns the match for current biome.
     */
    private static TriggerCandidate evaluateBiomeTriggers(Level level, BlockPos playerPos) {
        var biomeHolder = level.getBiome(playerPos);
        String biomeId = Objects.requireNonNull(level.registryAccess().lookupOrThrow(Registries.BIOME)
                .getKey(biomeHolder.value())).toString();

        Optional<BiomeTriggerDefinition> triggerOpt = ScentTriggerConfigLoader.getBiomeTrigger(biomeId);

        if (triggerOpt.isPresent()) {
            BiomeTriggerDefinition trigger = triggerOpt.get();

            TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
            double intensity = trigger.getIntensityOrDefault(settings.getBiomeIntensity());

            ScentTrigger scentTrigger = ScentTrigger.fromPassiveMode(
                trigger.getScentName(),
                trigger.getPriority(),
                -1,
                intensity
            );

            String source = "biome:" + biomeId;
            String displayName = getBiomeDisplayName(biomeId);

            return new TriggerCandidate(scentTrigger, source, displayName, TriggerType.BIOME);
        }

        return null;
    }

    /**
     * Activates the candidate trigger if it's different from the current one.
     */
    private static void activateIfChanged(Player player, TriggerCandidate candidate) {
        // Check if this is a different trigger than what's currently active
        if (currentTriggerSource != null && currentTriggerSource.equals(candidate.source)) {
            // Same source, no change needed
            return;
        }

        // Different trigger - activate it
        currentPassiveTrigger = candidate.trigger;
        currentTriggerSource = candidate.source;
        currentTriggerType = candidate.type;

        ScentTriggerManager.getInstance().trigger(candidate.trigger);

        // Get trigger type name for message
        String triggerTypeName = candidate.type.name().toLowerCase();

        // Send chat message to player
        player.displayClientMessage(
            Component.literal("§6[Aroma Affect] §7Scent: §e" + candidate.trigger.scentName() +
                " §7(" + triggerTypeName + ": §b" + candidate.displayName + "§7)"),
            false
        );

        AromaAffect.LOGGER.debug("Passive-mode activated: {} from {}",
            candidate.trigger.scentName(), candidate.source);
    }

    /**
     * Finds a nearby block of the specified type within range.
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
            AromaAffect.LOGGER.warn("Error searching for block {}: {}", blockId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Gets the ServerLevel from a Level (works for integrated server).
     */
    private static ServerLevel getServerLevel(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel;
        }

        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            return minecraft.getSingleplayerServer().getLevel(level.dimension());
        }

        return null;
    }

    /**
     * Checks if the player is near a specific structure type.
     */
    private static boolean isNearStructure(ServerLevel level, BlockPos playerPos, String structureId, int range) {
        try {
            ResourceLocation structureLocation = ResourceLocation.parse(structureId);

            var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            var structureOpt = structureRegistry.getOptional(structureLocation);

            if (structureOpt.isEmpty()) {
                return false;
            }

            Structure structure = structureOpt.get();

            int chunkRange = (range / 16) + 1;
            SectionPos playerSection = SectionPos.of(playerPos);

            for (int cx = -chunkRange; cx <= chunkRange; cx++) {
                for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                    int chunkX = playerSection.x() + cx;
                    int chunkZ = playerSection.z() + cz;

                    StructureStart structureStart = level.structureManager()
                        .getStartForStructure(
                            SectionPos.of(chunkX, 0, chunkZ),
                            structure,
                            level.getChunk(chunkX, chunkZ)
                        );

                    if (structureStart != null && structureStart.isValid()) {
                        var boundingBox = structureStart.getBoundingBox();

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
            AromaAffect.LOGGER.debug("Error checking structure {}: {}", structureId, e.getMessage());
        }

        return false;
    }

    /**
     * Gets a display name for a block ID.
     */
    private static String getBlockDisplayName(Level level, String blockId) {
        try {
            ResourceLocation location = ResourceLocation.parse(blockId);
            Optional<Block> blockOpt = level.registryAccess()
                .lookupOrThrow(Registries.BLOCK)
                .getOptional(location);

            if (blockOpt.isPresent()) {
                Block block = blockOpt.get();
                String displayName = block.getDescriptionId();
                if (displayName.startsWith("block.")) {
                    displayName = displayName.substring(6);
                }
                displayName = formatResourceName(displayName);
                return displayName;
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.debug("Error getting block display name for {}: {}", blockId, e.getMessage());
        }

        return formatResourceId(blockId);
    }

    /**
     * Gets a display name for a biome ID.
     */
    private static String getBiomeDisplayName(String biomeId) {
        return formatResourceId(biomeId);
    }

    /**
     * Formats a resource ID into a readable display name.
     */
    private static String formatResourceId(String resourceId) {
        try {
            ResourceLocation location = ResourceLocation.parse(resourceId);
            String path = location.getPath();
            return formatResourceName(path);
        } catch (Exception e) {
            return resourceId;
        }
    }

    /**
     * Formats a resource name by replacing underscores with spaces and capitalizing words.
     */
    private static String formatResourceName(String name) {
        String formatted = name.replace('_', ' ');
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
     */
    private static void clearPassiveScent(Player player) {
        if (currentPassiveTrigger != null) {
            ScentTriggerManager manager = ScentTriggerManager.getInstance();
            manager.getActiveScentOptional()
                .filter(trigger -> trigger.source() == ScentTriggerSource.PASSIVE_MODE)
                .ifPresent(trigger -> {
                    manager.stop();
                    AromaAffect.LOGGER.debug("Passive-mode scent stopped: {}", trigger.scentName());
                });

            currentPassiveTrigger = null;
            currentTriggerSource = null;
            currentTriggerType = null;
        }
    }

    /**
     * Stops passive-mode for a specific player.
     * Called externally when needed (e.g., player disconnects).
     */
    public static void stopPassiveMode() {
        if (currentPassiveTrigger != null) {
            ScentTriggerManager.getInstance().stop();
            currentPassiveTrigger = null;
            currentTriggerSource = null;
            currentTriggerType = null;
        }
    }

    /**
     * Gets the cooldown in milliseconds for the currently active trigger type.
     * Returns 0 if no trigger is active.
     *
     * @return cooldown in milliseconds, or 0 if no active trigger
     */
    public static long getCurrentCooldownMs() {
        if (currentTriggerType == null) {
            return 0;
        }
        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
        return getCooldownForType(currentTriggerType, settings);
    }

    /**
     * Gets the current trigger type name (block, structure, biome).
     *
     * @return the trigger type name, or null if no active trigger
     */
    public static String getCurrentTriggerTypeName() {
        return currentTriggerType != null ? currentTriggerType.name().toLowerCase() : null;
    }
}
