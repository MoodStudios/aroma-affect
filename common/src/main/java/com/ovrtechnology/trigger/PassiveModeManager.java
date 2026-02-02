package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.config.BiomeTriggerDefinition;
import com.ovrtechnology.trigger.config.BlockTriggerDefinition;
import com.ovrtechnology.trigger.config.MobTriggerDefinition;
import com.ovrtechnology.trigger.config.PassiveModeConfig;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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
 * blocks, mobs, biomes, or structures. It can be toggled on/off via the radial menu.</p>
 * <ul>
 *   <li>OVR hardware must be connected (or DEV_MODE enabled)</li>
 *   <li>User can toggle passive mode via the radial menu button</li>
 * </ul>
 *
 * <p><b>Priority System:</b></p>
 * <ul>
 *   <li>Blocks (MEDIUM) - highest priority, override everything</li>
 *   <li>Mobs (MEDLOW) - override structures and biomes</li>
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

        // Check if passive mode is enabled by user
        if (!isPassiveModeEnabled()) {
            clearPassiveScent(player);
            return;
        }

        // Only check every N ticks for performance
        if (++tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        AromaAffect.LOGGER.debug("[PassiveModeManager] Processing tick (every {} ticks)", CHECK_INTERVAL_TICKS);

        // CRITICAL CHECKS - Early returns for performance

        // Check if OVR hardware is connected
        if (!OvrWebSocketClient.getInstance().isConnected()) {
            AromaAffect.LOGGER.debug("Passive-mode disabled: OVR hardware not connected");
            clearPassiveScent(player);
            return;
        }

        // Passive mode now works regardless of nose equipped state
        // User controls it via the radial menu toggle button

        // Check if our passive trigger was replaced by a higher priority trigger that has since expired
        // This allows passive mode to "retake" control after item use or other triggers finish
        if (currentPassiveTrigger != null) {
            ScentTrigger activeScent = ScentTriggerManager.getInstance().getActiveScent();
            if (activeScent == null || activeScent.source() != ScentTriggerSource.PASSIVE_MODE) {
                // Our trigger was replaced or expired - clear our state so we can re-evaluate
                AromaAffect.LOGGER.debug("Passive trigger was replaced or expired, clearing state to re-evaluate");
                currentPassiveTrigger = null;
                currentTriggerSource = null;
                currentTriggerType = null;
            }
        }

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
            activateTrigger(player, blockCandidate);
            return;
        }

        // 2. Check mobs (MEDLOW priority)
        TriggerCandidate mobCandidate = evaluateMobTriggers(level, player);
        if (mobCandidate != null && canActivateScent(mobCandidate)) {
            activateTrigger(player, mobCandidate);
            return;
        }

        // 3. Check structures (MEDLOW priority)
        TriggerCandidate structureCandidate = evaluateStructureTriggers(level, playerPos);
        if (structureCandidate != null && canActivateScent(structureCandidate)) {
            activateTrigger(player, structureCandidate);
            return;
        }

        // 4. Check biomes (LOW priority - lowest)
        TriggerCandidate biomeCandidate = evaluateBiomeTriggers(level, playerPos);
        if (biomeCandidate != null && canActivateScent(biomeCandidate)) {
            activateTrigger(player, biomeCandidate);
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
        MOB,
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
        TriggerType type,
        int range,
        double distance
    ) {}

    /**
     * Checks if a scent can be activated (respects per-type cooldown).
     *
     * @param candidate the trigger candidate to check
     * @return true if the scent can be activated
     */
    private static boolean canActivateScent(TriggerCandidate candidate) {
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
            case MOB -> settings.getMobCooldownMs();
            case STRUCTURE -> settings.getStructureCooldownMs();
            case BIOME -> settings.getBiomeCooldownMs();
        };
    }

    /**
     * Minimum intensity value (at maximum range).
     */
    private static final double MIN_INTENSITY = 0.1;

    /**
     * Calculates intensity based on distance from trigger source.
     * Closer distance = higher intensity.
     *
     * @param distance current distance to the trigger
     * @param maxRange maximum detection range
     * @param maxIntensity the configured max intensity (at distance 0)
     * @return calculated intensity between MIN_INTENSITY and maxIntensity
     */
    private static double calculateIntensityByDistance(double distance, double maxRange, double maxIntensity) {
        if (maxRange <= 0) {
            return maxIntensity;
        }

        // Clamp distance to range
        distance = Math.max(0, Math.min(distance, maxRange));

        // Linear interpolation: at distance 0 -> maxIntensity, at maxRange -> MIN_INTENSITY
        double ratio = distance / maxRange;
        double intensity = maxIntensity - (ratio * (maxIntensity - MIN_INTENSITY));

        // Clamp result to valid range
        return Math.max(MIN_INTENSITY, Math.min(1.0, intensity));
    }

    /**
     * Evaluates block triggers and returns the first match.
     * Intensity scales with distance - closer blocks have higher intensity.
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
                // Calculate distance to the found block
                double distance = Math.sqrt(playerPos.distSqr(foundPos.get()));

                TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
                double maxIntensity = trigger.getIntensityOrDefault(settings.getBlockIntensity());

                // Calculate intensity based on distance
                double intensity = calculateIntensityByDistance(distance, range, maxIntensity);

                ScentTrigger scentTrigger = ScentTrigger.fromPassiveMode(
                    trigger.getScentName(),
                    trigger.getPriority(),
                    -1,
                    intensity
                );

                String source = "block:" + blockId;
                String displayName = getBlockDisplayName(level, blockId);

                return new TriggerCandidate(scentTrigger, source, displayName, TriggerType.BLOCK, range, distance);
            }
        }
        return null;
    }

    /**
     * Evaluates mob triggers and returns the first match.
     * Intensity scales with distance - closer mobs have higher intensity.
     */
    private static TriggerCandidate evaluateMobTriggers(Level level, Player player) {
        for (MobTriggerDefinition trigger : ScentTriggerConfigLoader.getAllMobTriggers()) {
            if (!trigger.isValid()) {
                continue;
            }

            String entityTypeId = trigger.getEntityType();
            int range = trigger.getRange();

            Optional<Entity> foundEntity = findNearbyMob(level, player, entityTypeId, range);

            if (foundEntity.isPresent()) {
                // Calculate distance to the found mob
                double distance = player.distanceTo(foundEntity.get());

                TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
                double maxIntensity = trigger.getIntensityOrDefault(settings.getMobIntensity());

                // Calculate intensity based on distance
                double intensity = calculateIntensityByDistance(distance, range, maxIntensity);

                ScentTrigger scentTrigger = ScentTrigger.fromPassiveMode(
                    trigger.getScentName(),
                    trigger.getPriority(),
                    -1,
                    intensity
                );

                String source = "mob:" + entityTypeId;
                String displayName = getMobDisplayName(foundEntity.get());

                return new TriggerCandidate(scentTrigger, source, displayName, TriggerType.MOB, range, distance);
            }
        }
        return null;
    }

    /**
     * Finds a nearby mob of the specified type within range.
     */
    private static Optional<Entity> findNearbyMob(Level level, Player player, String entityTypeId, int range) {
        try {
            ResourceLocation entityLocation = ResourceLocation.parse(entityTypeId);
            Optional<EntityType<?>> entityTypeOpt = level.registryAccess()
                .lookupOrThrow(Registries.ENTITY_TYPE)
                .getOptional(entityLocation);

            if (entityTypeOpt.isEmpty()) {
                return Optional.empty();
            }

            EntityType<?> targetType = entityTypeOpt.get();

            // Create search box around player
            AABB searchBox = player.getBoundingBox().inflate(range);

            // Find entities of the target type
            var entities = level.getEntities(player, searchBox, entity ->
                entity.getType() == targetType && entity.isAlive()
            );

            if (!entities.isEmpty()) {
                // Return the closest one
                Entity closest = null;
                double closestDist = Double.MAX_VALUE;
                for (Entity entity : entities) {
                    double dist = player.distanceToSqr(entity);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = entity;
                    }
                }
                return Optional.ofNullable(closest);
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.warn("Error searching for mob {}: {}", entityTypeId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Gets a display name for an entity.
     */
    private static String getMobDisplayName(Entity entity) {
        return entity.getType().getDescription().getString();
    }

    /**
     * Evaluates structure triggers and returns the first match.
     * Intensity scales with distance - closer structures have higher intensity.
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

            var distanceOpt = getDistanceToStructure(serverLevel, playerPos, structureId, range);
            if (distanceOpt.isPresent()) {
                double distance = distanceOpt.getAsDouble();

                TriggerSettings settings = ScentTriggerConfigLoader.getSettings();
                double maxIntensity = trigger.getIntensityOrDefault(settings.getStructureIntensity());

                // Calculate intensity based on distance
                double intensity = calculateIntensityByDistance(distance, range, maxIntensity);

                ScentTrigger scentTrigger = ScentTrigger.fromPassiveMode(
                    trigger.getScentName(),
                    trigger.getPriority(),
                    -1,
                    intensity
                );

                String source = "structure:" + structureId;
                String displayName = formatResourceId(structureId);

                return new TriggerCandidate(scentTrigger, source, displayName, TriggerType.STRUCTURE, range, distance);
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

            // Biomes don't have range/distance concept - player is "inside" the biome
            return new TriggerCandidate(scentTrigger, source, displayName, TriggerType.BIOME, 0, 0);
        }

        return null;
    }

    /**
     * Activates the candidate trigger.
     * Always sends the trigger to hardware (cooldown was already checked).
     * Only shows chat message when the source changes to avoid spam.
     */
    private static void activateTrigger(Player player, TriggerCandidate candidate) {
        boolean sourceChanged = currentTriggerSource == null || !currentTriggerSource.equals(candidate.source);

        // Update state
        currentPassiveTrigger = candidate.trigger;
        currentTriggerSource = candidate.source;
        currentTriggerType = candidate.type;

        // Always send trigger to hardware (cooldown already verified)
        ScentTriggerManager.getInstance().trigger(candidate.trigger);

        // Only show chat message when source changes (avoid spam every 15 seconds)
        if (sourceChanged) {
            // Get trigger type name for message
            String triggerTypeName = candidate.type.name().toLowerCase();

            // Format intensity as percentage
            int intensityPercent = (int) Math.round(candidate.trigger.intensity() * 100);

            // Build message based on trigger type
            String message;
            if (candidate.type == TriggerType.BIOME) {
                // Biomes don't have range/distance
                message = String.format("§6[Aroma Affect] §7Scent: §e%s §7(%s: §b%s§7) §8[%d%%]",
                    candidate.trigger.scentName(),
                    triggerTypeName,
                    candidate.displayName,
                    intensityPercent
                );
            } else {
                // Blocks, mobs, structures have range and distance
                message = String.format("§6[Aroma Affect] §7Scent: §e%s §7(%s: §b%s§7) §8[%d%% | %.1fm / %dm]",
                    candidate.trigger.scentName(),
                    triggerTypeName,
                    candidate.displayName,
                    intensityPercent,
                    candidate.distance,
                    candidate.range
                );
            }

            // Send chat message to player
            player.displayClientMessage(Component.literal(message), false);
        }

        AromaAffect.LOGGER.debug("Passive-mode activated: {} from {} (intensity: {}%, distance: {}, range: {}, sourceChanged: {})",
            candidate.trigger.scentName(), candidate.source,
            (int) Math.round(candidate.trigger.intensity() * 100),
            candidate.distance, candidate.range, sourceChanged);
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
     * Gets the distance to a specific structure type, if within range.
     *
     * @return OptionalDouble with distance if structure is in range, empty otherwise
     */
    private static java.util.OptionalDouble getDistanceToStructure(ServerLevel level, BlockPos playerPos, String structureId, int range) {
        try {
            ResourceLocation structureLocation = ResourceLocation.parse(structureId);

            var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            var structureOpt = structureRegistry.getOptional(structureLocation);

            if (structureOpt.isEmpty()) {
                return java.util.OptionalDouble.empty();
            }

            Structure structure = structureOpt.get();

            int chunkRange = (range / 16) + 1;
            SectionPos playerSection = SectionPos.of(playerPos);

            double closestDistance = Double.MAX_VALUE;

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

                        if (distance <= range && distance < closestDistance) {
                            closestDistance = distance;
                        }
                    }
                }
            }

            if (closestDistance < Double.MAX_VALUE) {
                return java.util.OptionalDouble.of(closestDistance);
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.debug("Error checking structure {}: {}", structureId, e.getMessage());
        }

        return java.util.OptionalDouble.empty();
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
     * Checks if passive mode is currently enabled.
     *
     * @return true if passive mode is enabled
     */
    public static boolean isPassiveModeEnabled() {
        return PassiveModeConfig.getInstance().isPassiveModeEnabled();
    }

    /**
     * Sets whether passive mode is enabled.
     * The state is persisted to disk.
     *
     * @param enabled true to enable passive mode, false to disable
     */
    public static void setPassiveModeEnabled(boolean enabled) {
        PassiveModeConfig config = PassiveModeConfig.getInstance();
        config.setPassiveModeEnabled(enabled);
        config.save();

        if (!enabled) {
            stopPassiveMode();
        }
        AromaAffect.LOGGER.info("Passive mode {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Toggles passive mode on/off.
     */
    public static void togglePassiveMode() {
        setPassiveModeEnabled(!isPassiveModeEnabled());
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
