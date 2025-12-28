package com.ovrtechnology.command.path;

import com.ovrtechnology.AromaCraft;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
 * </ul>
 */
public final class ActivePathManager {

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
        AromaCraft.LOGGER.info("Active path manager initialized");
    }

    /**
     * Creates a new active path for a player.
     * If the player already has an active path, it will be replaced.
     *
     * @param player      the player to create the path for
     * @param level       the server level
     * @param destination the destination position
     */
    public void createPath(ServerPlayer player, ServerLevel level, BlockPos destination) {
        UUID playerId = player.getUUID();

        // Remove any existing path for this player
        activePaths.remove(playerId);

        // Create new active path
        ActivePath path = new ActivePath(playerId, level.dimension().location(), destination);
        activePaths.put(playerId, path);

        AromaCraft.LOGGER.debug("Created active path for player {} to {}", player.getName().getString(), destination);
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

            if (distanceToDestination <= ARRIVAL_THRESHOLD) {
                // Player has arrived!
                player.sendSystemMessage(Component.literal(
                        "§6[AromaCraft] §aYou have arrived at your destination!"
                ));
                iterator.remove();
                continue;
            }

            // Spawn particles along the path
            spawnPathParticles(player, level, playerPos, destination);
        }
    }

    /**
     * Spawns undulating particles along a terrain-following path.
     *
     * @param player      the player to send particles to
     * @param level       the server level
     * @param origin      the starting position (player's position)
     * @param destination the target destination
     */
    private void spawnPathParticles(ServerPlayer player, ServerLevel level, BlockPos origin, BlockPos destination) {
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
        ParticleOptions particleType = ParticleTypes.GLOW;

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
                    0.1f,           // xDist: small random offset
                    0.1f,           // yDist
                    0.1f,           // zDist
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
    }

    /**
     * Represents an active particle path.
     *
     * @param playerId    the player's UUID
     * @param dimension   the dimension resource location
     * @param destination the destination block position
     */
    public record ActivePath(
            UUID playerId,
            net.minecraft.resources.ResourceLocation dimension,
            BlockPos destination
    ) {}
}

