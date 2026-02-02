package com.ovrtechnology.command.path;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.command.sub.PathSubCommand;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.scent.ScentDefinition;
import com.ovrtechnology.scent.ScentRegistry;
import com.ovrtechnology.trigger.ScentPriority;
import com.ovrtechnology.trigger.config.BiomeTriggerDefinition;
import com.ovrtechnology.trigger.config.BlockTriggerDefinition;
import com.ovrtechnology.trigger.config.ScentTriggerConfigLoader;
import com.ovrtechnology.trigger.config.StructureTriggerDefinition;
import com.ovrtechnology.trigger.config.TriggerSettings;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active particle paths for players.
 * <p>
 * This manager handles persistent particle paths that:
 * <ul>
 *   <li>Follow the terrain (using heightmaps)</li>
 *   <li>Have wave-like undulations for visual appeal</li>
 *   <li>Persist until the player reaches the destination</li>
 *   <li>Automatically clean up when players disconnect or change dimensions</li>
 *   <li>Trigger scents related to the tracked target</li>
 * </ul>
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
     * How often to spawn particles (in ticks). Every 5 ticks = 4 times per second.
     */
    private static final int PARTICLE_SPAWN_INTERVAL = 5;

    /**
     * Distance between path sample points (in blocks).
     */
    private static final double PATH_SAMPLE_SPACING = 2.0;

    /**
     * Amplitude of the wave undulations (in blocks).
     */
    private static final double WAVE_AMPLITUDE = 0.8;

    /**
     * Frequency of the wave undulations (controls how many waves appear).
     */
    private static final double WAVE_FREQUENCY = 0.15;

    /**
     * Height offset above ground for particles.
     */
    private static final double PARTICLE_HEIGHT_OFFSET = 1.0;

    /**
     * Maximum path length to render (in blocks).
     * Limits rendering for very long paths to maintain performance.
     */
    private static final double MAX_RENDER_DISTANCE = 150.0;

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
    public void createPath(ServerPlayer player, ServerLevel level, BlockPos destination) {
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
    public void createPath(ServerPlayer player, ServerLevel level, BlockPos destination,
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

        // Only process particles at the defined interval
        if (tickCounter % PARTICLE_SPAWN_INTERVAL != 0) {
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

            ServerLevel level = player.level();
            BlockPos playerPos = player.blockPosition();
            BlockPos destination = path.destination();

            // Check if player has arrived
            double distanceToDestination = Math.sqrt(
                    Math.pow(playerPos.getX() - destination.getX(), 2) +
                    Math.pow(playerPos.getZ() - destination.getZ(), 2)
            );

            // Send distance update to client
            PathScentNetworking.sendDistanceUpdate(player, (int) distanceToDestination);

            if (distanceToDestination <= ARRIVAL_THRESHOLD) {
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

            // Spawn particles along the path
            spawnPathParticles(player, level, playerPos, destination, path);

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
     * Resolves a particle color from the scent associated with a path.
     * Falls back to a soft white if no scent mapping is found.
     */
    private ParticleOptions resolveParticleForPath(ActivePath path) {
        if (path.targetType() != null && path.targetId() != null) {
            Optional<String> scentName = getScentForTarget(path.targetType(), path.targetId());
            if (scentName.isPresent()) {
                String name = scentName.get();
                // Try lookup by fallback name first, then by ID
                Optional<ScentDefinition> def = ScentRegistry.getScentByName(name);
                if (def.isEmpty()) {
                    def = ScentRegistry.getScent(name.toLowerCase().replace(" ", "_"));
                }
                if (def.isPresent()) {
                    int[] rgb = def.get().getColorRGB();
                    int argb = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
                    return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, argb);
                }
            }
        }
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFFFFFFFF);
    }

    /**
     * Spawns undulating particles along a terrain-following path.
     *
     * @param player      the player to send particles to
     * @param level       the server level
     * @param origin      the starting position (player's position)
     * @param destination the target destination
     * @param path        the active path (used to resolve particle color)
     */
    private void spawnPathParticles(ServerPlayer player, ServerLevel level, BlockPos origin, BlockPos destination, ActivePath path) {
        double dx = destination.getX() - origin.getX();
        double dz = destination.getZ() - origin.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Limit the render distance
        double renderDistance = Math.min(horizontalDistance, MAX_RENDER_DISTANCE);

        // Calculate direction
        double dirX = dx / horizontalDistance;
        double dirZ = dz / horizontalDistance;

        // Perpendicular direction for wave offset
        double perpX = -dirZ;
        double perpZ = dirX;

        int numberOfPoints = Math.max(10, (int) (renderDistance / PATH_SAMPLE_SPACING));
        ParticleOptions particleType = resolveParticleForPath(path);

        // Animation offset based on tick counter for flowing effect
        double animationOffset = (tickCounter * 0.1) % (2 * Math.PI);

        for (int i = 0; i <= numberOfPoints; i++) {
            double progress = (double) i / numberOfPoints;
            double distance = progress * renderDistance;

            // Base position along the path
            double baseX = origin.getX() + dirX * distance;
            double baseZ = origin.getZ() + dirZ * distance;

            // Calculate wave offset (sinusoidal undulation)
            double wavePhase = distance * WAVE_FREQUENCY + animationOffset;
            double waveOffset = Math.sin(wavePhase) * WAVE_AMPLITUDE;

            // Apply wave offset perpendicular to path direction
            double x = baseX + perpX * waveOffset + 0.5;
            double z = baseZ + perpZ * waveOffset + 0.5;

            // Get terrain height at this position
            int terrainY = getTerrainHeight(level, (int) x, (int) z);
            double y = terrainY + PARTICLE_HEIGHT_OFFSET;

            // Create and send particle packet
            ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                    particleType,
                    false,          // overrideLimiter
                    true,           // alwaysShow: show particles at distance
                    x,
                    y,
                    z,
                    0.05f,          // xDist: small random offset
                    0.05f,          // yDist
                    0.05f,          // zDist
                    0.0f,           // maxSpeed
                    1               // count
            );

            player.connection.send(packet);
        }
    }

    /**
     * Gets the terrain height at a given position.
     *
     * @param level the server level
     * @param x     the x coordinate
     * @param z     the z coordinate
     * @return the y level of the terrain surface
     */
    private int getTerrainHeight(ServerLevel level, int x, int z) {
        try {
            // Use MOTION_BLOCKING for a height that entities can stand on
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        } catch (Exception e) {
            // Fallback to sea level if height lookup fails
            return level.getSeaLevel();
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

