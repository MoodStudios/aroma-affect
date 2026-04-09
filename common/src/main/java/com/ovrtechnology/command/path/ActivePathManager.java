package com.ovrtechnology.command.path;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.block.BlockRegistry;
import com.ovrtechnology.command.sub.PathSubCommand;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.scent.ScentDefinition;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
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

    /** Cooldown for block-mined scent triggers (10 seconds). */
    private static final long BLOCK_MINED_SCENT_COOLDOWN_MS = 10000;
    private static final Map<UUID, Long> blockMinedScentCooldowns = new HashMap<>();

    /**
     * How often to process paths (in ticks). Every 5 ticks = 4 times per second.
     */
    private static final int TICK_INTERVAL = 5;

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

        // Create new active path
        ActivePath path = new ActivePath(playerId, level.dimension().location(), destination, targetType, targetId);
        activePaths.put(playerId, path);

        AromaAffect.LOGGER.debug("Created active path for player {} to {} (target: {} {})",
                player.getName().getString(), destination,
                targetType != null ? targetType : "none",
                targetId != null ? targetId : "");

        // riron sticky popup dismissed when ore is actually mined (TutorialOreDropHandler)
    }

    /**
     * Removes the active path for a player.
     *
     * @param playerId the player's UUID
     */
    public void removePath(UUID playerId) {
        activePaths.remove(playerId);
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

            // Check if player still has a nose equipped
            if (!EquippedNoseHelper.hasNoseEquipped(player)) {
                PathScentNetworking.sendPathNotFound(player, "Nose unequipped");
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

            // Determine arrival based on target type
            boolean arrived;
            if (path.targetType() == TargetType.BIOME && path.targetId() != null) {
                // Biome tracking: arrived when player enters the target biome
                var currentBiome = player.level().getBiome(playerPos);
                var biomeKey = currentBiome.unwrapKey().orElse(null);
                arrived = biomeKey != null && biomeKey.location().toString().equals(path.targetId());
            } else if (path.targetType() == TargetType.BLOCK && path.targetId() != null) {
                // Block tracking: arrived when the target block is mined (no longer matches)
                var blockState = player.level().getBlockState(destination);
                var blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
                arrived = !blockId.equals(path.targetId());
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

                // Trigger scent when block is mined (with 10s cooldown)
                if (path.targetType() == TargetType.BLOCK && path.targetId() != null) {
                    triggerBlockMinedScent(player, path.targetId());
                }

                PathScentNetworking.sendPathArrived(player);
                iterator.remove();
                continue;
            }

        }
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

    /**
     * Triggers the scent associated with a mined block, respecting cooldown.
     */
    private void triggerBlockMinedScent(ServerPlayer player, String blockId) {
        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();

        Long lastTime = blockMinedScentCooldowns.get(playerId);
        if (lastTime != null && now - lastTime < BLOCK_MINED_SCENT_COOLDOWN_MS) {
            return; // On cooldown
        }

        Optional<ScentDefinition> scentOpt = BlockRegistry.getScentForBlock(blockId);
        if (scentOpt.isEmpty()) {
            return;
        }

        String scentName = scentOpt.get().getFallbackName();
        blockMinedScentCooldowns.put(playerId, now);
        TutorialScentZoneNetworking.sendScentTrigger(player, scentName, 1.0, "block_mined");
        AromaAffect.LOGGER.info("Block mined scent '{}' triggered for player {} (block: {})",
                scentName, player.getName().getString(), blockId);
    }
}
