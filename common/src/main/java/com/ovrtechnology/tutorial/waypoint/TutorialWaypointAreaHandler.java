package com.ovrtechnology.tutorial.waypoint;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.command.path.ActivePathManager;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.lookup.LookupTarget;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for waypoint area detection.
 * <p>
 * When a player enters a waypoint's defined area (cuboid), the waypoint
 * trail is automatically deactivated for that player.
 * <p>
 * This allows tutorial creators to define "arrival zones" where the
 * visual guide disappears once the player reaches their destination.
 */
public final class TutorialWaypointAreaHandler {

    /**
     * Tracks which waypoint each player currently has active.
     * Key: Player UUID, Value: Waypoint ID
     */
    private static final Map<UUID, String> activeWaypoints = new ConcurrentHashMap<>();

    /**
     * Tracks which waypoints each player has "completed" (entered the area).
     * Key: Player UUID, Value: Set of completed waypoint IDs
     */
    private static final Map<UUID, Set<String>> completedWaypoints = new ConcurrentHashMap<>();

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10; // Check every 10 ticks (0.5 seconds)

    private TutorialWaypointAreaHandler() {
    }

    /**
     * Initializes the area handler.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL) {
                return;
            }
            tickCounter = 0;

            // Check all players on all levels
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayerArea(player);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial waypoint area handler initialized");
    }

    /**
     * Marks a waypoint as active for a player.
     * Called when a waypoint is activated via command.
     *
     * @param playerId   the player's UUID
     * @param waypointId the waypoint ID
     */
    public static void setActiveWaypoint(UUID playerId, String waypointId) {
        activeWaypoints.put(playerId, waypointId);
    }

    /**
     * Clears the active waypoint for a player.
     *
     * @param playerId the player's UUID
     */
    public static void clearActiveWaypoint(UUID playerId) {
        activeWaypoints.remove(playerId);
    }

    /**
     * Gets the active waypoint for a player.
     *
     * @param playerId the player's UUID
     * @return the waypoint ID, or null if none active
     */
    public static String getActiveWaypoint(UUID playerId) {
        return activeWaypoints.get(playerId);
    }

    /**
     * Checks if a player has completed a specific waypoint.
     *
     * @param playerId   the player's UUID
     * @param waypointId the waypoint ID
     * @return true if completed
     */
    public static boolean hasCompletedWaypoint(UUID playerId, String waypointId) {
        Set<String> completed = completedWaypoints.get(playerId);
        return completed != null && completed.contains(waypointId);
    }

    /**
     * Resets all waypoint progress for a player.
     *
     * @param playerId the player's UUID
     */
    public static void resetPlayer(UUID playerId) {
        activeWaypoints.remove(playerId);
        completedWaypoints.remove(playerId);
        AromaAffect.LOGGER.debug("Reset waypoint progress for player {}", playerId);
    }

    /**
     * Resets all waypoint progress for all players.
     */
    public static void resetAll() {
        activeWaypoints.clear();
        completedWaypoints.clear();
        AromaAffect.LOGGER.debug("Reset waypoint progress for all players");
    }

    /**
     * Checks if a player is inside their active waypoint's area.
     */
    private static void checkPlayerArea(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // Check if tutorial mode is active
        if (!TutorialModule.isActive(level)) {
            return;
        }

        UUID playerId = player.getUUID();
        String activeWaypointId = activeWaypoints.get(playerId);

        if (activeWaypointId == null) {
            return;
        }

        // Get the waypoint
        Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, activeWaypointId);
        if (wpOpt.isEmpty()) {
            return;
        }

        TutorialWaypoint waypoint = wpOpt.get();

        // Check if waypoint has an area defined
        if (!waypoint.hasArea()) {
            return;
        }

        // Check if player is inside the area
        BlockPos playerPos = player.blockPosition();
        if (waypoint.isInsideArea(playerPos)) {
            // Player reached the destination!
            onPlayerReachedArea(player, waypoint);
        }
    }

    /**
     * Called when a player enters a waypoint's area.
     */
    private static void onPlayerReachedArea(ServerPlayer player, TutorialWaypoint waypoint) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        UUID playerId = player.getUUID();
        String waypointId = waypoint.getId();

        // Mark as completed
        completedWaypoints
                .computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(waypointId);

        // Clear active waypoint
        activeWaypoints.remove(playerId);

        // Play level up sound
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                1.0f,
                1.0f
        );

        AromaAffect.LOGGER.info("Player {} reached waypoint {} area", player.getName().getString(), waypointId);

        // Execute Oliver action if defined
        if (waypoint.hasOliverAction()) {
            AromaAffect.LOGGER.info("Executing Oliver action '{}' for waypoint {}", waypoint.getOliverAction(), waypointId);
            executeOliverAction(player, level, waypoint.getOliverAction());
        }

        // Activate cinematic if defined
        if (waypoint.hasActivateCinematic()) {
            String cinematicId = waypoint.getActivateCinematicId();
            TutorialCinematicHandler.startCinematic(player, cinematicId);
            AromaAffect.LOGGER.debug("Activated cinematic {} for player {}", cinematicId, player.getName().getString());
        }

        // Determine next waypoint using nose-conditional chains
        // Priority: noseChain > defaultNextWaypointId > nextWaypointId
        String equippedNoseId = EquippedNoseHelper.getEquippedNoseId(player).orElse(null);
        String nextWaypointId = waypoint.getNextWaypointForNose(equippedNoseId);

        if (nextWaypointId != null && !nextWaypointId.isEmpty()) {
            // Activate the next waypoint
            Optional<TutorialWaypoint> nextWpOpt = TutorialWaypointManager.getWaypoint(level, nextWaypointId);
            if (nextWpOpt.isPresent() && nextWpOpt.get().isComplete()) {
                TutorialWaypoint nextWp = nextWpOpt.get();

                // Set active waypoint
                activeWaypoints.put(playerId, nextWaypointId);

                // Send next waypoint to client
                TutorialWaypointNetworking.sendWaypointToPlayer(player, nextWaypointId, nextWp.getValidPositions());

                if (equippedNoseId != null && waypoint.getNoseChains().containsKey(equippedNoseId)) {
                    AromaAffect.LOGGER.debug("Nose-chained to waypoint: {} -> {} (nose: {})", waypointId, nextWaypointId, equippedNoseId);
                } else {
                    AromaAffect.LOGGER.debug("Chained to next waypoint: {} -> {}", waypointId, nextWaypointId);
                }
            } else {
                // Next waypoint doesn't exist or is incomplete, just clear
                TutorialWaypointNetworking.sendClearToPlayer(player);
                AromaAffect.LOGGER.warn("Next waypoint {} not found or incomplete", nextWaypointId);
            }
        } else {
            // No next waypoint, just clear
            TutorialWaypointNetworking.sendClearToPlayer(player);
        }
    }

    /**
     * Finds the nearest Oliver entity to a player.
     */
    private static TutorialOliverEntity findNearestOliver(ServerPlayer player, ServerLevel level) {
        AABB searchArea = new AABB(
                player.getX() - 100, player.getY() - 50, player.getZ() - 100,
                player.getX() + 100, player.getY() + 50, player.getZ() + 100
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class,
                searchArea
        );

        if (olivers.isEmpty()) {
            return null;
        }

        TutorialOliverEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (TutorialOliverEntity oliver : olivers) {
            double dist = oliver.distanceToSqr(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = oliver;
            }
        }

        return nearest;
    }

    /**
     * Executes Oliver action(s) when a waypoint is completed.
     * <p>
     * Multiple actions can be separated by semicolon (;).
     * <p>
     * Supported actions:
     * <ul>
     *   <li>"follow" - Oliver follows the player</li>
     *   <li>"stop" - Oliver becomes stationary</li>
     *   <li>"walkto:x,y,z" - Oliver walks to position</li>
     *   <li>"dialogue:id" - Set Oliver's dialogue and open it</li>
     *   <li>"trade:id" - Set Oliver's trade</li>
     *   <li>"cleartrade" - Clear Oliver's trade</li>
     *   <li>"lookup:blockId" - Trigger block lookup</li>
     * </ul>
     */
    private static void executeOliverAction(ServerPlayer player, ServerLevel level, String action) {
        TutorialOliverEntity oliver = findNearestOliver(player, level);
        if (oliver == null) {
            AromaAffect.LOGGER.warn("Cannot execute Oliver action '{}': no Oliver found within 100 blocks of player {}", action, player.getName().getString());
            return;
        }
        AromaAffect.LOGGER.info("Found Oliver at {}, executing action '{}'", oliver.blockPosition(), action);

        // Support multiple actions separated by semicolon
        String[] actions = action.split(";");
        String pendingDialogueId = null;

        for (String singleAction : actions) {
            singleAction = singleAction.trim();
            if (singleAction.isEmpty()) continue;

            String actionLower = singleAction.toLowerCase();

            if (actionLower.equals("follow")) {
                oliver.setFollowing(player);
                AromaAffect.LOGGER.debug("Oliver action: following player {}", player.getName().getString());
            } else if (actionLower.equals("stop")) {
                oliver.setStationary();
                AromaAffect.LOGGER.debug("Oliver action: stopped");
            } else if (actionLower.startsWith("walkto:")) {
                String coordsStr = singleAction.substring(7);
                String[] parts = coordsStr.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());
                        oliver.setWalkingTo(new BlockPos(x, y, z));
                        AromaAffect.LOGGER.debug("Oliver action: walking to {}, {}, {}", x, y, z);
                    } catch (NumberFormatException e) {
                        AromaAffect.LOGGER.warn("Invalid walkto coordinates: {}", coordsStr);
                    }
                } else {
                    AromaAffect.LOGGER.warn("Invalid walkto format: {}", singleAction);
                }
            } else if (actionLower.startsWith("dialogue:")) {
                // Defer dialogue opening until after all other actions (especially trade:)
                pendingDialogueId = singleAction.substring(9).trim();
                oliver.setDialogueId(pendingDialogueId);
            } else if (actionLower.startsWith("trade:")) {
                String tradeId = singleAction.substring(6).trim();
                oliver.setTradeId(tradeId);
                AromaAffect.LOGGER.debug("Oliver action: trade set to {}", tradeId);
            } else if (actionLower.equals("cleartrade")) {
                oliver.setTradeId("");
                AromaAffect.LOGGER.debug("Oliver action: trade cleared");
            } else if (actionLower.startsWith("teleportplayer:")) {
                String coordsStr = singleAction.substring(15);
                String[] parts = coordsStr.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());
                        player.teleportTo(level, x + 0.5, y, z + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
                        AromaAffect.LOGGER.debug("Teleported player to {}, {}, {}", x, y, z);
                    } catch (NumberFormatException e) {
                        AromaAffect.LOGGER.warn("Invalid teleportplayer coordinates: {}", coordsStr);
                    }
                }
            } else if (actionLower.startsWith("lookup:")) {
                String blockId = singleAction.substring(7).trim();
                triggerLookup(player, level, blockId);
            } else {
                AromaAffect.LOGGER.warn("Unknown Oliver action: {}", singleAction);
            }
        }

        // Open dialogue AFTER processing all actions (so trade is set first)
        if (pendingDialogueId != null) {
            AromaAffect.LOGGER.info("Sending open dialogue packet to player {} for dialogue '{}' (entityId: {}, hasTrade: {}, tradeId: '{}')",
                    player.getName().getString(), pendingDialogueId, oliver.getId(), oliver.hasTrade(), oliver.getTradeId());
            TutorialDialogueContentNetworking.sendOpenDialogue(
                    player, oliver.getId(), pendingDialogueId,
                    oliver.hasTrade(), oliver.getTradeId()
            );
        }
    }

    /**
     * Triggers an async block lookup for the player, creating a path trail to the found block.
     */
    private static void triggerLookup(ServerPlayer player, ServerLevel level, String blockId) {
        LookupTarget target = LookupTarget.block(ResourceLocation.parse(blockId));
        BlockPos origin = player.blockPosition();

        LookupManager.getInstance().lookupAsync(level, origin, target, result -> {
            if (result.isSuccess()) {
                BlockPos destination = result.getPosition();
                ActivePathManager.getInstance().createPath(
                        player, level, destination,
                        ActivePathManager.TargetType.BLOCK, blockId
                );
                int distance = (int) Math.sqrt(origin.distSqr(destination));
                PathScentNetworking.sendPathFound(player, distance, destination);
                AromaAffect.LOGGER.debug("Tutorial lookup found {} at {} for player {}",
                        blockId, destination, player.getName().getString());
            } else {
                AromaAffect.LOGGER.warn("Tutorial lookup failed for {}: {}",
                        blockId, result.failureReason());
            }
        });
    }
}
