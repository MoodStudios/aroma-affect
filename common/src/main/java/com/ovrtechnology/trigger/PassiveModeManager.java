package com.ovrtechnology.trigger;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.trigger.config.BiomeTriggerDefinition;
import com.ovrtechnology.trigger.config.BlockTriggerDefinition;
import com.ovrtechnology.trigger.config.MobTriggerDefinition;
import com.ovrtechnology.trigger.config.ClientConfig;
import com.ovrtechnology.trigger.config.PassiveModeConfig;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.StructureTriggerDefinition;

import com.ovrtechnology.websocket.OvrWebSocketClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
    private static final boolean DEV_MODE = true;

    /**
     * How often to check for passive triggers (in ticks).
     * Checking every 20 ticks (~1 second).
     */
    private static final int CHECK_INTERVAL_TICKS = 20;

    /**
     * Gets the activation range for mob triggers from user config.
     */
    private static double getMobActivationRange() {
        return ClientConfig.getInstance().getPassiveMobRange();
    }

    /**
     * Gets the activation range for block triggers from user config.
     */
    private static double getBlockActivationRange() {
        return ClientConfig.getInstance().getPassiveBlockRange();
    }

    /**
     * Angle threshold for look-at detection (degrees).
     * Blocks and non-hostile mobs only trigger if the player is looking within this cone.
     */
    private static final double LOOK_AT_ANGLE_DEGREES = 30.0;

    /**
     * Pre-computed cosine of the look-at angle for efficient dot product comparison.
     */
    private static final double LOOK_AT_COS_THRESHOLD = Math.cos(Math.toRadians(LOOK_AT_ANGLE_DEGREES));

    /**
     * Number of ticks to wait after first tick before starting scans.
     * Allows the world to finish loading chunks before adding scan overhead.
     */
    private static final int STARTUP_DELAY_TICKS = 100; // ~5 seconds


    /**
     * Hostile mob entity types that can use MOB INTERRUPT.
     * Only these mobs can interrupt other triggers immediately (player safety).
     * Passive mobs (villagers, sheep, cows, etc.) follow normal cooldown rules.
     */
    private static final Set<String> HOSTILE_MOBS = Set.of(
        "minecraft:zombie", "minecraft:skeleton", "minecraft:creeper",
        "minecraft:spider", "minecraft:cave_spider", "minecraft:enderman",
        "minecraft:blaze", "minecraft:ghast", "minecraft:magma_cube",
        "minecraft:piglin", "minecraft:hoglin", "minecraft:strider",
        "minecraft:wither_skeleton", "minecraft:ender_dragon", "minecraft:wither",
        "minecraft:warden", "minecraft:elder_guardian"
    );

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

    /**
     * Last biome the player was in (for detecting biome changes).
     */
    private static String lastBiomeId = null;

    /**
     * Tracks the last trigger timestamp for each trigger type (BLOCK, MOB, etc.).
     * Enforces the user-configured per-type cooldown between any triggers of the same type.
     */
    private static final Map<TriggerType, Long> typeTriggerTimes = new HashMap<>();

    /**
     * Last structure the player was inside (for one-time structure triggers).
     * Structures only trigger once on entry, like biomes.
     */
    private static String lastStructureId = null;

    /**
     * Structure ID synced from the server via {@link StructureSyncHandler}.
     * {@code null} when the player is not inside any tracked structure.
     */
    private static volatile String serverStructureId = null;

    private static int tickCounter = 0;
    private static int startupTicksElapsed = 0;
    private static boolean startupComplete = false;

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

        // Wait for world to finish loading before starting scans
        if (!startupComplete) {
            if (++startupTicksElapsed < STARTUP_DELAY_TICKS) {
                return;
            }
            startupComplete = true;
            AromaAffect.LOGGER.info("[PassiveMode] Startup delay complete, beginning passive scans");
        }

        // Only check every N ticks for performance
        if (++tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        AromaAffect.LOGGER.debug("[PassiveModeManager] Processing tick (every {} ticks)", CHECK_INTERVAL_TICKS);

        // CRITICAL CHECKS - Early returns for performance

        // Check if OVR hardware is connected (skip in DEV_MODE)
        if (!DEV_MODE && !OvrWebSocketClient.getInstance().isConnected()) {
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


        // Evaluate ALL triggers to maintain tracking state (e.g., biome transitions)
        TriggerCandidate mobCandidate = evaluateMobTriggers(level, player);
        TriggerCandidate blockCandidate = evaluateBlockTriggers(level, playerPos, player);
        TriggerCandidate structureCandidate = evaluateStructureTriggers(level, playerPos);
        TriggerCandidate biomeCandidate = evaluateBiomeTriggers(level, playerPos);

        // Select highest priority candidate only (Mobs > Blocks > Structures > Biomes)
        // If a higher priority source exists nearby, lower priorities are suppressed
        // even if the higher priority is on cooldown — prevents ping-pong between triggers
        TriggerCandidate bestCandidate = mobCandidate;
        if (bestCandidate == null) bestCandidate = blockCandidate;
        if (bestCandidate == null) bestCandidate = structureCandidate;
        if (bestCandidate == null) bestCandidate = biomeCandidate;

        if (bestCandidate != null) {
            // Try to activate if cooldown has expired.
            // This ensures consistent re-trigger intervals (e.g., every 5s for mobs)
            // and updates intensity based on current distance.
            // The per-type cooldown in canActivateScent() prevents spam between triggers.
            if (canActivateScent(bestCandidate)) {
                activateTrigger(player, bestCandidate);
            }
            // Even if on cooldown, don't fall through to lower priorities
            return;
        }

        // No candidates found — player moved away from all triggers
        if (currentPassiveTrigger != null) {
            AromaAffect.LOGGER.debug("No trigger candidates nearby, clearing active scent");
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
     * Checks if a scent can be activated (respects cooldowns with special bypass rules).
     *
     * <p>Special rules:</p>
     * <ul>
     *   <li>First approach to a source: Triggers immediately</li>
     *   <li>Source recently triggered: Respects per-type cooldown</li>
     *   <li>Mobs can interrupt non-mob triggers on first appearance</li>
     * </ul>
     *
     * @param candidate the trigger candidate to check
     * @return true if the scent can be activated
     */
    private static boolean canActivateScent(TriggerCandidate candidate) {
        String scentName = candidate.trigger.scentName();
        long cooldownMs = getEffectiveCooldown(candidate);
        long now = System.currentTimeMillis();

        // MOB INTERRUPT: Hostile mobs can bypass cooldown to interrupt non-mob triggers
        if (candidate.type == TriggerType.MOB && isHostileMob(candidate.source)) {
            ScentTrigger activeScent = ScentTriggerManager.getInstance().getActiveScent();
            if (activeScent != null && activeScent.source() == ScentTriggerSource.PASSIVE_MODE
                && currentTriggerType != TriggerType.MOB) {
                if (DEV_MODE) {
                    AromaAffect.LOGGER.info("[PassiveMode] HOSTILE MOB INTERRUPT: {} at distance {}",
                        scentName, candidate.distance);
                }
                return true;
            }
        }

        // Per-type cooldown: enforces the user-configured cooldown between any triggers of the same type.
        // e.g., "Block Cooldown = 6s" means 6s between ANY two block triggers (coal → redstone → coal).
        Long lastTriggerTime = typeTriggerTimes.get(candidate.type);
        if (lastTriggerTime != null && (now - lastTriggerTime) < cooldownMs) {
            AromaAffect.LOGGER.debug("[PassiveMode] Blocked by type cooldown: {} ({}ms remaining)",
                candidate.type, cooldownMs - (now - lastTriggerTime));
            return false;
        }

        return true;
    }

    /**
     * Gets the appropriate cooldown duration for a trigger type.
     * Block and mob cooldowns come from ClientConfig (user-editable).
     * Structure and biome cooldowns come from TriggerSettings (data pack defaults).
     */
    private static long getCooldownForType(TriggerType type) {
        ClientConfig config = ClientConfig.getInstance();
        return switch (type) {
            case BLOCK -> config.getPassiveBlockCooldownMs();
            case MOB -> config.getPassiveMobCooldownMs();
            case STRUCTURE -> ScentTriggerConfigLoader.getSettings().getStructureCooldownMs();
            case BIOME -> ScentTriggerConfigLoader.getSettings().getBiomeCooldownMs();
        };
    }

    /**
     * Gets the effective cooldown for a candidate, considering:
     * - Passive mobs use a separate cooldown from hostile mobs
     */
    private static long getEffectiveCooldown(TriggerCandidate candidate) {
        // Passive mobs (villagers, sheep, cows) use their own cooldown
        if (candidate.type == TriggerType.MOB && !isHostileMob(candidate.source)) {
            return ClientConfig.getInstance().getPassivePassiveMobCooldownMs();
        }
        return getCooldownForType(candidate.type);
    }

    /**
     * Minimum intensity value (at maximum range).
     */
    private static final double MIN_INTENSITY = 0.3;

    /**
     * Distance threshold for maximum intensity (100%).
     * If player is within this distance, intensity is always 100%.
     */
    private static final double FULL_INTENSITY_DISTANCE = 1.5;

    /**
     * Calculates intensity based on distance from trigger source.
     * - Distance <= 1.5 blocks: 100% intensity (touching/very close)
     * - Distance > 1.5 to max range: scales down to MIN_INTENSITY
     *
     * @param distance current distance to the trigger
     * @param maxRange maximum detection range
     * @return calculated intensity between MIN_INTENSITY and 1.0
     */
    private static double calculateIntensityByDistance(double distance, double maxRange) {
        // Very close = 100% intensity
        if (distance <= FULL_INTENSITY_DISTANCE) {
            return 1.0;
        }

        if (maxRange <= FULL_INTENSITY_DISTANCE) {
            return 1.0;
        }

        // Scale from 100% at FULL_INTENSITY_DISTANCE to MIN_INTENSITY at maxRange
        double effectiveRange = maxRange - FULL_INTENSITY_DISTANCE;
        double effectiveDistance = distance - FULL_INTENSITY_DISTANCE;

        double ratio = effectiveDistance / effectiveRange;
        double intensity = 1.0 - (ratio * (1.0 - MIN_INTENSITY));

        // Clamp result to valid range
        return Math.max(MIN_INTENSITY, Math.min(1.0, intensity));
    }

    /**
     * Evaluates block triggers and returns the one the player is looking at most directly.
     * Scans every block position once and picks the individual block (not block type) with
     * the highest dot product to the player's view direction.
     */
    private static TriggerCandidate evaluateBlockTriggers(Level level, BlockPos playerPos, Player player) {
        int searchRange = (int) Math.ceil(getBlockActivationRange());
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 viewDir = player.getViewVector(1.0f).normalize();
        double activationRange = getBlockActivationRange();

        // Build lookup: Block instance → trigger definition (one-time per evaluation)
        Map<Block, BlockTriggerDefinition> triggersByBlock = new HashMap<>();
        for (BlockTriggerDefinition trigger : ScentTriggerConfigLoader.getAllBlockTriggers()) {
            if (!trigger.isProximityTrigger() || !trigger.isValid()) continue;
            try {
                ResourceLocation loc = ResourceLocation.parse(trigger.getBlockId());
                level.registryAccess().lookupOrThrow(Registries.BLOCK)
                    .getOptional(loc)
                    .ifPresent(block -> triggersByBlock.put(block, trigger));
            } catch (Exception e) {
                // skip invalid block IDs
            }
        }
        if (triggersByBlock.isEmpty()) return null;

        // Single scan: check every position, pick the block most directly looked at
        TriggerCandidate bestCandidate = null;
        double bestDot = -1;

        for (int x = -searchRange; x <= searchRange; x++) {
            for (int y = -searchRange; y <= searchRange; y++) {
                for (int z = -searchRange; z <= searchRange; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    Block block = level.getBlockState(checkPos).getBlock();

                    BlockTriggerDefinition trigger = triggersByBlock.get(block);
                    if (trigger == null) continue;

                    double distance = Math.sqrt(playerPos.distSqr(checkPos));
                    if (distance > activationRange) continue;

                    Vec3 toBlock = Vec3.atCenterOf(checkPos).subtract(eyePos).normalize();
                    double dot = viewDir.dot(toBlock);

                    if (dot >= LOOK_AT_COS_THRESHOLD && dot > bestDot) {
                        bestDot = dot;
                        double intensity = calculateIntensityByDistance(distance, activationRange);

                        ScentTrigger scentTrigger = ScentTrigger.fromPassiveMode(
                            trigger.getScentName(),
                            ScentPriority.MEDIUM,
                            -1,
                            intensity
                        );

                        String source = "block:" + trigger.getBlockId();
                        String displayName = getBlockDisplayName(level, trigger.getBlockId());

                        bestCandidate = new TriggerCandidate(scentTrigger, source, displayName,
                            TriggerType.BLOCK, (int) activationRange, distance);
                    }
                }
            }
        }
        return bestCandidate;
    }

    /**
     * Evaluates mob triggers and returns the first match.
     * Uses fixed activation range of 5 blocks for real-time detection.
     * Mobs have HIGH priority for player safety.
     */
    private static TriggerCandidate evaluateMobTriggers(Level level, Player player) {
        int searchRange = (int) Math.ceil(getMobActivationRange());

        for (MobTriggerDefinition trigger : ScentTriggerConfigLoader.getAllMobTriggers()) {
            if (!trigger.isValid()) {
                continue;
            }

            String entityTypeId = trigger.getEntityType();

            Optional<Entity> foundEntity = findNearbyMob(level, player, entityTypeId, searchRange);

            if (foundEntity.isPresent()) {
                double distance = player.distanceTo(foundEntity.get());

                // Only activate if within activation range
                // Non-hostile mobs also require the player to be looking at them
                if (distance <= getMobActivationRange()
                        && (HOSTILE_MOBS.contains(entityTypeId)
                            || isPlayerLookingAt(player, foundEntity.get().getEyePosition(1.0f)))) {
                    double intensity = calculateIntensityByDistance(distance, getMobActivationRange());

                    // Mobs use HIGH priority for player safety
                    ScentTrigger scentTrigger = ScentTrigger.fromPassiveMode(
                        trigger.getScentName(),
                        ScentPriority.HIGH,
                        -1,
                        intensity
                    );

                    String source = "mob:" + entityTypeId;
                    String displayName = getMobDisplayName(foundEntity.get());

                    return new TriggerCandidate(scentTrigger, source, displayName,
                        TriggerType.MOB, (int) getMobActivationRange(), distance);
                }
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
     * Evaluates structure triggers with one-time trigger behavior (like biomes).
     * Only fires once when the player enters a structure. Must leave and re-enter to trigger again.
     *
     * <p>Structure presence is synced from the server via {@link StructureSyncHandler},
     * so this works correctly on both singleplayer and dedicated multiplayer servers.</p>
     */
    private static TriggerCandidate evaluateStructureTriggers(Level level, BlockPos playerPos) {
        String currentStructureId = serverStructureId;

        if (currentStructureId == null) {
            // Not inside any tracked structure - reset tracking
            if (lastStructureId != null) {
                if (DEV_MODE) {
                    AromaAffect.LOGGER.info("[PassiveMode] Left structure: {}", lastStructureId);
                }
                lastStructureId = null;
            }
            return null;
        }

        // One-time trigger: only fire when entering a NEW structure
        if (currentStructureId.equals(lastStructureId)) {
            return null;
        }

        // New structure entered
        String previousStructure = lastStructureId;
        lastStructureId = currentStructureId;

        if (DEV_MODE) {
            AromaAffect.LOGGER.info("[PassiveMode] Entered structure: {} (previous: {})",
                currentStructureId, previousStructure);
        }

        // Find the matching trigger definition for scent name and priority
        for (StructureTriggerDefinition trigger : ScentTriggerConfigLoader.getAllStructureTriggers()) {
            if (!trigger.isProximityTrigger() || !trigger.isValid()) continue;
            if (!trigger.getStructureId().equals(currentStructureId)) continue;

            ScentTrigger scentTrigger = ScentTrigger.fromPassiveMode(
                trigger.getScentName(),
                ScentPriority.MEDLOW,
                -1,
                1.0
            );

            String source = "structure:" + currentStructureId;
            String displayName = formatResourceId(currentStructureId);

            return new TriggerCandidate(scentTrigger, source, displayName,
                TriggerType.STRUCTURE, 0, 0);
        }

        return null;
    }

    /**
     * Evaluates biome triggers and returns a trigger only when entering a NEW biome.
     * The trigger fires once when the player enters a biome, then only fires again
     * if they leave and return (or enter a different biome).
     */
    private static TriggerCandidate evaluateBiomeTriggers(Level level, BlockPos playerPos) {
        var biomeHolder = level.getBiome(playerPos);
        String currentBiomeId = Objects.requireNonNull(level.registryAccess().lookupOrThrow(Registries.BIOME)
                .getKey(biomeHolder.value())).toString();

        // Only trigger if biome changed
        if (currentBiomeId.equals(lastBiomeId)) {
            return null; // Same biome, don't trigger again
        }

        // Biome changed - update tracking
        String previousBiome = lastBiomeId;
        lastBiomeId = currentBiomeId;

        if (DEV_MODE) {
            AromaAffect.LOGGER.info("[PassiveMode] Biome changed: {} -> {}", previousBiome, currentBiomeId);
        }

        Optional<BiomeTriggerDefinition> triggerOpt = ScentTriggerConfigLoader.getBiomeTrigger(currentBiomeId);

        if (triggerOpt.isPresent()) {
            BiomeTriggerDefinition trigger = triggerOpt.get();

            // Biome triggers use full intensity (1.0) since player just entered
            double intensity = 1.0;

            ScentTrigger scentTrigger = ScentTrigger.fromPassiveMode(
                trigger.getScentName(),
                trigger.getPriority(),
                -1,
                intensity
            );

            String source = "biome:" + currentBiomeId;
            String displayName = getBiomeDisplayName(currentBiomeId);

            // Biomes don't have range/distance concept - player is "inside" the biome
            return new TriggerCandidate(scentTrigger, source, displayName, TriggerType.BIOME, 0, 0);
        }

        return null;
    }

    /**
     * Activates the candidate trigger.
     * Always sends the trigger to hardware (cooldown was already checked).
     * Only shows chat message in DEV_MODE for testing purposes.
     */
    private static void activateTrigger(Player player, TriggerCandidate candidate) {
        ScentTriggerManager manager = ScentTriggerManager.getInstance();

        // In passive mode, we need to clear any previous passive trigger first
        // This allows lower priority triggers to activate when higher priority sources are gone
        // (e.g., block can activate after mob leaves, even though block has lower priority)
        ScentTrigger currentActive = manager.getActiveScent();
        if (currentActive != null && currentActive.source() == ScentTriggerSource.PASSIVE_MODE) {
            // Clear previous passive trigger to allow new one regardless of priority
            manager.stop();
        }

        // Update state
        currentPassiveTrigger = candidate.trigger;
        currentTriggerSource = candidate.source;
        currentTriggerType = candidate.type;

        // Record trigger time for this type to enforce per-type cooldown
        typeTriggerTimes.put(candidate.type, System.currentTimeMillis());

        // Send trigger to hardware (cooldown already verified)
        boolean triggered = manager.trigger(candidate.trigger);

        if (DEV_MODE) {
            AromaAffect.LOGGER.info("[PassiveMode] activateTrigger: {} -> triggered={}", candidate.source, triggered);
        }

        // Only show chat message in DEV_MODE for testing
        if (DEV_MODE) {
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

        AromaAffect.LOGGER.debug("Passive-mode activated: {} from {} (intensity: {}%, distance: {}, range: {})",
            candidate.trigger.scentName(), candidate.source,
            (int) Math.round(candidate.trigger.intensity() * 100),
            candidate.distance, candidate.range);
    }

    /**
     * Finds the closest nearby block of the specified type within range.
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
            BlockPos closest = null;
            double closestDistSq = Double.MAX_VALUE;

            // Search in a cubic area around the player, keep the closest match
            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        BlockPos checkPos = center.offset(x, y, z);
                        BlockState state = level.getBlockState(checkPos);

                        if (state.is(targetBlock)) {
                            double distSq = center.distSqr(checkPos);
                            if (distSq < closestDistSq) {
                                closestDistSq = distSq;
                                closest = checkPos;
                            }
                        }
                    }
                }
            }
            return Optional.ofNullable(closest);
        } catch (Exception e) {
            AromaAffect.LOGGER.warn("Error searching for block {}: {}", blockId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Called from the client-side S2C packet receiver when the server sends
     * an updated structure presence for this player.
     *
     * @param structureId the structure the player is inside, or {@code null} if none
     */
    public static void setServerStructureId(String structureId) {
        serverStructureId = structureId;
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
     * Checks if a mob source represents a hostile mob.
     * Only hostile mobs can use MOB INTERRUPT to override other triggers.
     *
     * @param source the trigger source (e.g., "mob:minecraft:zombie")
     * @return true if the mob is hostile
     */
    private static boolean isHostileMob(String source) {
        if (source == null || !source.startsWith("mob:")) return false;
        String entityType = source.substring(4); // Remove "mob:" prefix
        return HOSTILE_MOBS.contains(entityType);
    }

    /**
     * Checks if the player is looking at a target position within the configured cone of vision.
     * Uses dot product between the player's view direction and the direction to the target.
     *
     * @param player the player whose view direction to check
     * @param targetPos the world position to check against
     * @return true if the target is within the look-at cone (30 degrees)
     */
    private static boolean isPlayerLookingAt(Player player, Vec3 targetPos) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 viewDir = player.getViewVector(1.0f).normalize();
        Vec3 toTarget = targetPos.subtract(eyePos).normalize();
        double dot = viewDir.dot(toTarget);
        return dot >= LOOK_AT_COS_THRESHOLD;
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
            lastStructureId = null;
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
            lastStructureId = null;
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
        return getCooldownForType(currentTriggerType);
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
