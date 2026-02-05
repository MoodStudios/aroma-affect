package com.ovrtechnology.command.path;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.command.sub.PathSubCommand;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.config.BiomeTriggerDefinition;
import com.ovrtechnology.trigger.config.BlockTriggerDefinition;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.StructureTriggerDefinition;
import com.ovrtechnology.trigger.config.TriggerSettings;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active paths for players (server-side).
 * <p>
 * Handles path lifecycle: creation, arrival detection (3D), distance updates,
 * and scent triggers. All visual effects (trail rendering, particles, pulse
 * animations) are handled entirely client-side by {@code PathTrailRenderer}.
 */
public final class ActivePathManager {

    /**
     * Types of targets that can be tracked.
     */
    public enum TargetType {
        BLOCK,
        BIOME,
        STRUCTURE
    }

    private static final ActivePathManager INSTANCE = new ActivePathManager();

    /**
     * Distance threshold to consider the player has arrived (in blocks).
     */
    private static final double ARRIVAL_THRESHOLD = 5.0;

    /**
     * How often to process paths (in ticks). Every 5 ticks = 4 times per second.
     */
    private static final int TICK_INTERVAL = 5;

    /**
     * Default duration for path tracking scent triggers (in ticks).
     * 5 seconds = 100 ticks.
     */
    private static final int PATH_SCENT_DURATION_TICKS = 100;

    /**
     * Map of player UUIDs to their last scent trigger time.
     * Used to enforce cooldowns between scent triggers during path tracking.
     */
    private final Map<UUID, Long> lastScentTriggerTime = new ConcurrentHashMap<>();

    private final Map<UUID, ActivePath> activePaths = new ConcurrentHashMap<>();
    private int tickCounter = 0;
    private boolean initialized = false;

    private ActivePathManager() {}

    public static ActivePathManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the path manager.
     * Should be called during mod initialization.
     */
    public static void init() {
        if (INSTANCE.initialized) {
            return;
        }

        TickEvent.SERVER_POST.register(INSTANCE::onServerTick);
        INSTANCE.initialized = true;
        AromaAffect.LOGGER.info("Active path manager initialized");
    }

    /**
     * Creates a new active path for a player without target info (legacy support).
     * If the player already has an active path, it will be replaced.
     *
     * @param player      the player to create the path for
     * @param level       the server level
     * @param destination the destination position
     */
    public void createPath(ServerPlayer player, net.minecraft.server.level.ServerLevel level, BlockPos destination) {
        createPath(player, level, destination, null, null);
    }

    /**
     * Creates a new active path for a player with target tracking information.
     * If the player already has an active path, it will be replaced.
     *
     * @param player      the player to create the path for
     * @param level       the server level
     * @param destination the destination position
     * @param targetType  the type of target being tracked (can be null)
     * @param targetId    the ID of the target (e.g., "minecraft:diamond_ore", can be null)
     */
    public void createPath(ServerPlayer player, net.minecraft.server.level.ServerLevel level, BlockPos destination,
                           TargetType targetType, String targetId) {
        UUID playerId = player.getUUID();

        // Remove any existing path for this player
        activePaths.remove(playerId);
        lastScentTriggerTime.remove(playerId);

        // Create new active path
        ActivePath path = new ActivePath(playerId, level.dimension().location(), destination, targetType, targetId);
        activePaths.put(playerId, path);

        AromaAffect.LOGGER.debug("Created active path for player {} to {} (target: {} {})",
                player.getName().getString(), destination,
                targetType != null ? targetType : "none",
                targetId != null ? targetId : "");

        // Trigger initial scent immediately
        if (targetType != null && targetId != null) {
            triggerPathScent(player, targetType, targetId);
            lastScentTriggerTime.put(playerId, System.currentTimeMillis());
        }
    }

    /**
     * Removes the active path for a player.
     *
     * @param playerId the player's UUID
     */
    public void removePath(UUID playerId) {
        activePaths.remove(playerId);
        lastScentTriggerTime.remove(playerId);
    }

    /**
     * Checks if a player has an active path.
     *
     * @param playerId the player's UUID
     * @return true if the player has an active path
     */
    public boolean hasActivePath(UUID playerId) {
        return activePaths.containsKey(playerId);
    }

    private void onServerTick(MinecraftServer server) {
        tickCounter++;

        if (tickCounter % TICK_INTERVAL != 0) {
            return;
        }

        Iterator<Map.Entry<UUID, ActivePath>> iterator = activePaths.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ActivePath> entry = iterator.next();
            UUID playerId = entry.getKey();
            ActivePath path = entry.getValue();

            // Get the player
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                // Player disconnected, remove path
                iterator.remove();
                continue;
            }

            // Check if player is in the same dimension
            if (!player.level().dimension().location().equals(path.dimension())) {
                // Player changed dimension, remove path
                iterator.remove();
                continue;
            }

            BlockPos playerPos = player.blockPosition();
            BlockPos destination = path.destination();

            // Check if player has arrived (3D distance — prevents false arrival above underground targets)
            double distanceToDestination = Math.sqrt(
                    Math.pow(playerPos.getX() - destination.getX(), 2) +
                    Math.pow(playerPos.getY() - destination.getY(), 2) +
                    Math.pow(playerPos.getZ() - destination.getZ(), 2)
            );

            // Send distance update to client
            PathScentNetworking.sendDistanceUpdate(player, (int) distanceToDestination);

            // Biome tracking: arrived when player enters the target biome (not exact point)
            boolean arrived;
            if (path.targetType() == TargetType.BIOME && path.targetId() != null) {
                var currentBiome = player.level().getBiome(playerPos);
                var biomeKey = currentBiome.unwrapKey().orElse(null);
                arrived = biomeKey != null && biomeKey.location().toString().equals(path.targetId());
            } else {
                arrived = distanceToDestination <= ARRIVAL_THRESHOLD;
            }

            if (arrived) {
                // Player has arrived!
                if (PathSubCommand.isVerbose()) {
                    player.sendSystemMessage(Component.literal(
                            "§6[Aroma Affect] §aYou have arrived at your destination!"
                    ));
                }
                PathScentNetworking.sendPathArrived(player);
                iterator.remove();
                continue;
            }

            // Check if we should trigger a scent (based on cooldown)
            if (path.targetType() != null && path.targetId() != null) {
                checkAndTriggerScent(player, path);
            }
        }
    }

    /**
     * Checks if the cooldown has passed and triggers a scent for the path.
     */
    private void checkAndTriggerScent(ServerPlayer player, ActivePath path) {
        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();

        Long lastTrigger = lastScentTriggerTime.get(playerId);
        long cooldownMs = ScentTriggerConfigLoader.getSettings().getPathTrackingCooldownMs();

        if (lastTrigger == null || (now - lastTrigger) >= cooldownMs) {
            triggerPathScent(player, path.targetType(), path.targetId());
            lastScentTriggerTime.put(playerId, now);
        }
    }

    /**
     * Triggers a scent based on the target type and ID.
     * Uses networking to send the trigger from server to client.
     *
     * @param player     the player to trigger the scent for
     * @param targetType the type of target being tracked
     * @param targetId   the ID of the target
     */
    private void triggerPathScent(ServerPlayer player, TargetType targetType, String targetId) {
        Optional<String> scentName = getScentForTarget(targetType, targetId);

        if (scentName.isEmpty()) {
            AromaAffect.LOGGER.debug("No scent mapping found for {} {}", targetType, targetId);
            return;
        }

        String scent = scentName.get();
        double intensity = getIntensityForTarget(targetType, targetId);
        ScentPriority priority = ScentPriority.MEDIUM;

        // Send scent trigger to client via networking
        PathScentNetworking.sendScentTrigger(player, scent, intensity, priority);

        AromaAffect.LOGGER.debug("Sent path tracking scent '{}' to player {} (tracking {} {})",
                scent, player.getName().getString(), targetType, targetId);
    }

    /**
     * Gets the scent name for a target based on its type and ID.
     */
    private Optional<String> getScentForTarget(TargetType targetType, String targetId) {
        return switch (targetType) {
            case BLOCK -> ScentTriggerConfigLoader.getBlockTrigger(targetId)
                    .map(BlockTriggerDefinition::getScentName);
            case BIOME -> ScentTriggerConfigLoader.getBiomeTrigger(targetId)
                    .map(BiomeTriggerDefinition::getScentName);
            case STRUCTURE -> ScentTriggerConfigLoader.getStructureTrigger(targetId)
                    .map(StructureTriggerDefinition::getScentName);
        };
    }

    /**
     * Gets the intensity for a target based on its type and ID.
     */
    private double getIntensityForTarget(TargetType targetType, String targetId) {
        TriggerSettings settings = ScentTriggerConfigLoader.getSettings();

        return switch (targetType) {
            case BLOCK -> ScentTriggerConfigLoader.getBlockTrigger(targetId)
                    .map(def -> def.getIntensityOrDefault(settings.getBlockIntensity()))
                    .orElse(settings.getBlockIntensity());
            case BIOME -> ScentTriggerConfigLoader.getBiomeTrigger(targetId)
                    .map(def -> def.getIntensityOrDefault(settings.getBiomeIntensity()))
                    .orElse(settings.getBiomeIntensity());
            case STRUCTURE -> ScentTriggerConfigLoader.getStructureTrigger(targetId)
                    .map(def -> def.getIntensityOrDefault(settings.getStructureIntensity()))
                    .orElse(settings.getStructureIntensity());
        };
    }

    /**
     * Gets the number of active paths.
     *
     * @return the number of active paths
     */
    public int getActivePathCount() {
        return activePaths.size();
    }

    /**
     * Clears all active paths.
     * Used when server is stopping.
     */
    public void clearAll() {
        activePaths.clear();
        lastScentTriggerTime.clear();
    }

    /**
     * Represents an active particle path.
     *
     * @param playerId    the player's UUID
     * @param dimension   the dimension resource location
     * @param destination the destination block position
     * @param targetType  the type of target being tracked (can be null)
     * @param targetId    the ID of the target (can be null)
     */
    public record ActivePath(
            UUID playerId,
            net.minecraft.resources.ResourceLocation dimension,
            BlockPos destination,
            TargetType targetType,
            String targetId
    ) {}
}
