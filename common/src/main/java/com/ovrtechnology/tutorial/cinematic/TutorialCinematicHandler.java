package com.ovrtechnology.tutorial.cinematic;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.command.path.ActivePathManager;
import com.ovrtechnology.lookup.LookupManager;
import com.ovrtechnology.lookup.LookupTarget;
import com.ovrtechnology.network.PathScentNetworking;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.tutorial.animation.TutorialAnimationHandler;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles active cinematics for players.
 * <p>
 * This handler:
 * <ul>
 *   <li>Tracks active cinematics per player</li>
 *   <li>Advances frames on server tick</li>
 *   <li>Puts players in spectator mode during cinematics</li>
 *   <li>Smoothly interpolates camera between positions (like a video)</li>
 *   <li>Sends title/subtitle packets to clients</li>
 *   <li>Executes Oliver actions and sounds</li>
 *   <li>Restores player state on completion</li>
 * </ul>
 */
public final class TutorialCinematicHandler {

    /**
     * Active cinematics per player.
     * Key: Player UUID, Value: Cinematic state
     */
    private static final Map<UUID, CinematicPlayerState> activeCinematics = new ConcurrentHashMap<>();
    private static final Set<UUID> loopingCinematics = ConcurrentHashMap.newKeySet();

    private static boolean initialized = false;

    private TutorialCinematicHandler() {
    }

    /**
     * Initializes the cinematic handler.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            // Process all active cinematics
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayerCinematic(player);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial cinematic handler initialized");
    }

    /**
     * Starts a cinematic for a player.
     *
     * @param player      the player
     * @param cinematicId the cinematic ID
     * @return true if started, false if cinematic not found or already in cinematic
     */
    public static boolean startCinematic(ServerPlayer player, String cinematicId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        // Check if already in a cinematic
        if (activeCinematics.containsKey(player.getUUID())) {
            AromaAffect.LOGGER.debug("Player {} already in a cinematic", player.getName().getString());
            return false;
        }

        // Get the cinematic
        Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, cinematicId);
        if (cinematicOpt.isEmpty()) {
            AromaAffect.LOGGER.warn("Cinematic {} not found", cinematicId);
            return false;
        }

        TutorialCinematic cinematic = cinematicOpt.get();

        // Check for nose override
        String equippedNoseId = EquippedNoseHelper.getEquippedNoseId(player).orElse(null);
        String actualCinematicId = cinematic.getCinematicIdForNose(equippedNoseId);

        // If there's an override, get the actual cinematic
        if (!actualCinematicId.equals(cinematicId)) {
            cinematicOpt = TutorialCinematicManager.getCinematic(level, actualCinematicId);
            if (cinematicOpt.isEmpty()) {
                AromaAffect.LOGGER.warn("Override cinematic {} not found", actualCinematicId);
                return false;
            }
            cinematic = cinematicOpt.get();
            cinematicId = actualCinematicId;
        }

        if (!cinematic.hasFrames()) {
            AromaAffect.LOGGER.warn("Cinematic {} has no frames", cinematicId);
            return false;
        }

        // Store original player state
        CinematicPlayerState state = new CinematicPlayerState(
                cinematicId,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                player.gameMode.getGameModeForPlayer()
        );

        if (!state.initFirstFrame(cinematic)) {
            return false;
        }

        activeCinematics.put(player.getUUID(), state);

        // Put player in spectator mode for the cinematic
        player.setGameMode(GameType.SPECTATOR);

        // Process first frame (set camera position, show titles)
        processFrame(player, level, cinematic, state);

        // Immediately teleport to camera position
        teleportToCamera(player, level, state);

        AromaAffect.LOGGER.debug("Started cinematic {} for player {}", cinematicId, player.getName().getString());
        return true;
    }

    /**
     * Starts a cinematic for a player with optional looping.
     *
     * @param player      the player
     * @param cinematicId the cinematic ID
     * @param loop        if true, the cinematic will restart when it completes
     * @return true if started, false if cinematic not found or already in cinematic
     */
    public static boolean startCinematic(ServerPlayer player, String cinematicId, boolean loop) {
        boolean started = startCinematic(player, cinematicId);
        if (started && loop) {
            loopingCinematics.add(player.getUUID());
        }
        return started;
    }

    /**
     * Stops any active cinematic for a player.
     *
     * @param player the player
     * @return true if stopped, false if no active cinematic
     */
    public static boolean stopCinematic(ServerPlayer player) {
        CinematicPlayerState state = activeCinematics.get(player.getUUID());
        if (state == null) {
            return false;
        }

        try {
            // Restore player state
            restorePlayerState(player, state);

            // Clear titles
            clearTitles(player);
        } finally {
            // Always remove from active cinematics, even if restoration fails
            activeCinematics.remove(player.getUUID());
            loopingCinematics.remove(player.getUUID());
        }

        AromaAffect.LOGGER.debug("Stopped cinematic for player {}", player.getName().getString());
        return true;
    }

    /**
     * Checks if a player is currently in a cinematic.
     *
     * @param player the player
     * @return true if in cinematic
     */
    public static boolean isInCinematic(ServerPlayer player) {
        return activeCinematics.containsKey(player.getUUID());
    }

    /**
     * Gets the active cinematic ID for a player.
     *
     * @param player the player
     * @return the cinematic ID, or null if not in a cinematic
     */
    public static String getActiveCinematicId(ServerPlayer player) {
        CinematicPlayerState state = activeCinematics.get(player.getUUID());
        return state != null ? state.getCinematicId() : null;
    }

    /**
     * Processes a tick for a player's cinematic.
     */
    private static void tickPlayerCinematic(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        CinematicPlayerState state = activeCinematics.get(player.getUUID());
        if (state == null) {
            return;
        }

        // ALWAYS teleport to interpolated camera position on every tick
        teleportToCamera(player, level, state);

        // Tick the frame (also advances interpolation)
        if (!state.tick()) {
            // Frame finished, advance to next
            Optional<TutorialCinematic> cinematicOpt = TutorialCinematicManager.getCinematic(level, state.getCinematicId());
            if (cinematicOpt.isEmpty()) {
                // Cinematic deleted mid-playback
                stopCinematic(player);
                return;
            }

            TutorialCinematic cinematic = cinematicOpt.get();

            if (!state.advanceFrame(cinematic)) {
                // Cinematic complete
                onCinematicComplete(player, level, cinematic, state);
                return;
            }

            // Process new frame (updates camera position in state)
            processFrame(player, level, cinematic, state);
        }
    }

    /**
     * Teleports the player to the interpolated camera position.
     */
    private static void teleportToCamera(ServerPlayer player, ServerLevel level, CinematicPlayerState state) {
        if (!state.hasTargetCameraPosition()) {
            return;
        }

        // Get interpolated position (smoothly moves between frames)
        Double x = state.getInterpolatedCameraX();
        Double y = state.getInterpolatedCameraY();
        Double z = state.getInterpolatedCameraZ();
        Float yaw = state.getInterpolatedCameraYaw();
        Float pitch = state.getInterpolatedCameraPitch();

        if (x == null || y == null || z == null) {
            return;
        }

        // Teleport with rotation
        player.teleportTo(
                level,
                x,
                y,
                z,
                Set.of(),
                yaw != null ? yaw : player.getYRot(),
                pitch != null ? pitch : player.getXRot(),
                false
        );
    }

    /**
     * Processes a cinematic frame.
     */
    private static void processFrame(ServerPlayer player, ServerLevel level, TutorialCinematic cinematic, CinematicPlayerState state) {
        CinematicFrame frame = cinematic.getFrame(state.getCurrentFrameIndex());
        if (frame == null) {
            return;
        }

        // Update camera position in state (will interpolate over fadeIn ticks)
        if (frame.hasCameraPosition()) {
            state.setTargetCameraPosition(
                    frame.cameraX(),
                    frame.cameraY(),
                    frame.cameraZ(),
                    frame.cameraYaw(),
                    frame.cameraPitch(),
                    frame.fadeIn() // Use fadeIn as transition duration
            );
        } else {
            state.clearCameraPosition();
        }

        // Send titles and execute actions only once per frame
        if (!state.isFrameActionExecuted()) {
            state.setFrameActionExecuted(true);

            // Send title animation
            player.connection.send(new ClientboundSetTitlesAnimationPacket(
                    frame.fadeIn(),
                    frame.duration(),
                    frame.fadeOut()
            ));

            // Send title
            if (frame.title() != null && !frame.title().isEmpty()) {
                Component titleComponent = Component.literal(frame.title())
                        .withStyle(style -> style.withColor(frame.titleColor()));
                player.connection.send(new ClientboundSetTitleTextPacket(titleComponent));
            } else {
                // Clear title if no text
                player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
            }

            // Send subtitle
            if (frame.subtitle() != null && !frame.subtitle().isEmpty()) {
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal(frame.subtitle())
                ));
            } else {
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
            }

            // Play sound
            if (frame.sound() != null && !frame.sound().isEmpty()) {
                playSound(player, frame.sound());
            }

            // Execute Oliver action
            if (frame.oliverAction() != null && !frame.oliverAction().isEmpty()) {
                executeOliverAction(player, level, frame.oliverAction());
            }
        }
    }

    /**
     * Called when a cinematic completes.
     */
    private static void onCinematicComplete(ServerPlayer player, ServerLevel level, TutorialCinematic cinematic, CinematicPlayerState state) {
        // If looping, restart the cinematic instead of completing
        if (loopingCinematics.contains(player.getUUID())) {
            activeCinematics.remove(player.getUUID());
            // Re-init state for a fresh loop
            CinematicPlayerState newState = new CinematicPlayerState(
                    state.getCinematicId(),
                    state.getOriginalX(), state.getOriginalY(), state.getOriginalZ(),
                    state.getOriginalYaw(), state.getOriginalPitch(),
                    state.getOriginalGameMode()
            );
            if (newState.initFirstFrame(cinematic)) {
                activeCinematics.put(player.getUUID(), newState);
                processFrame(player, level, cinematic, newState);
                teleportToCamera(player, level, newState);
                AromaAffect.LOGGER.debug("Looping cinematic {} for player {}", cinematic.getId(), player.getName().getString());
            }
            return;
        }

        // Restore player state first, then remove from active cinematics
        try {
            restorePlayerState(player, state);
            clearTitles(player);
        } finally {
            activeCinematics.remove(player.getUUID());
        }

        AromaAffect.LOGGER.debug("Cinematic {} completed for player {}", cinematic.getId(), player.getName().getString());

        // Execute on-complete Oliver action
        if (cinematic.hasOnCompleteOliverAction()) {
            executeOliverAction(player, level, cinematic.getOnCompleteOliverAction());
        }

        // Activate on-complete waypoint
        if (cinematic.hasOnCompleteWaypoint()) {
            String waypointId = cinematic.getOnCompleteWaypointId();
            Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, waypointId);
            if (wpOpt.isPresent() && wpOpt.get().isComplete()) {
                TutorialWaypoint wp = wpOpt.get();
                TutorialWaypointAreaHandler.setActiveWaypoint(player.getUUID(), waypointId);
                TutorialWaypointNetworking.sendWaypointToPlayer(player, waypointId, wp.getValidPositions());
                AromaAffect.LOGGER.debug("Activated waypoint {} after cinematic", waypointId);
            } else {
                AromaAffect.LOGGER.warn("On-complete waypoint {} not found or incomplete", waypointId);
            }
        }

        // Play on-complete animation
        if (cinematic.hasOnCompleteAnimation()) {
            TutorialAnimationHandler.play(level, cinematic.getOnCompleteAnimationId());
        }
    }

    /**
     * Restores the player to their original state before the cinematic.
     */
    private static void restorePlayerState(ServerPlayer player, CinematicPlayerState state) {
        ServerLevel level = (ServerLevel) player.level();

        // Teleport back to original position
        player.teleportTo(
                level,
                state.getOriginalX(),
                state.getOriginalY(),
                state.getOriginalZ(),
                Set.of(),
                state.getOriginalYaw(),
                state.getOriginalPitch(),
                false
        );

        // Restore original game mode
        player.setGameMode(state.getOriginalGameMode());
    }

    /**
     * Clears title display for a player.
     */
    private static void clearTitles(ServerPlayer player) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 0, 0));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
    }

    /**
     * Plays a sound for a player.
     */
    private static void playSound(ServerPlayer player, String soundId) {
        try {
            ResourceLocation soundLoc = ResourceLocation.parse(soundId);
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundLoc);
            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    soundEvent,
                    SoundSource.MASTER,
                    1.0f,
                    1.0f
            );
        } catch (Exception e) {
            AromaAffect.LOGGER.warn("Failed to play sound {}: {}", soundId, e.getMessage());
        }
    }

    /**
     * Executes an Oliver action.
     */
    private static void executeOliverAction(ServerPlayer player, ServerLevel level, String action) {
        TutorialOliverEntity oliver = findNearestOliver(player, level);
        if (oliver == null) {
            AromaAffect.LOGGER.warn("Cannot execute Oliver action '{}': no Oliver found nearby", action);
            return;
        }

        String actionLower = action.toLowerCase();

        if (actionLower.equals("follow")) {
            oliver.setFollowing(player);
        } else if (actionLower.equals("stop")) {
            oliver.setStationary();
        } else if (actionLower.startsWith("walkto:")) {
            String coordsStr = action.substring(7);
            String[] parts = coordsStr.split(",");
            if (parts.length == 3) {
                try {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    int z = Integer.parseInt(parts[2].trim());
                    oliver.setWalkingTo(new BlockPos(x, y, z));
                } catch (NumberFormatException e) {
                    AromaAffect.LOGGER.warn("Invalid walkto coordinates: {}", coordsStr);
                }
            }
        } else if (actionLower.startsWith("dialogue:")) {
            String dialogueId = action.substring(9).trim();
            oliver.setDialogueId(dialogueId);
            TutorialDialogueContentNetworking.sendOpenDialogue(
                    player, oliver.getId(), dialogueId,
                    oliver.hasTrade(), oliver.getTradeId()
            );
        } else if (actionLower.startsWith("trade:")) {
            String tradeId = action.substring(6).trim();
            oliver.setTradeId(tradeId);
        } else if (actionLower.startsWith("lookup:")) {
            String blockId = action.substring(7).trim();
            triggerLookup(player, level, blockId);
        } else if (actionLower.startsWith("teleportplayer:")) {
            String coordsStr = action.substring(15);
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
        }
    }

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
}
