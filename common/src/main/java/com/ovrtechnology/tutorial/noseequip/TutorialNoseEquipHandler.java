package com.ovrtechnology.tutorial.noseequip;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.nose.EquippedNoseHelper;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.animation.TutorialAnimationHandler;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects when a player equips a nose and fires configured triggers.
 * <p>
 * Checks HEAD slot each tick for changes. Each trigger fires only once per player.
 */
public final class TutorialNoseEquipHandler {

    // Tracks the last known nose ID per player (null = no nose)
    private static final Map<UUID, String> lastEquippedNose = new HashMap<>();

    // Tracks which triggers have already fired for each player (prevents repeat firing)
    private static final Map<UUID, Set<String>> firedTriggers = new ConcurrentHashMap<>();

    // Delayed actions for epic teleport sequences
    private static final List<DelayedAction> delayedActions = new ArrayList<>();

    private static boolean initialized = false;

    private TutorialNoseEquipHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        TickEvent.SERVER_LEVEL_POST.register(TutorialNoseEquipHandler::onServerTick);
        TickEvent.SERVER_POST.register(server -> { if (ReplayCompat.isReplayServer(server)) return; processDelayedActions(); });
        AromaAffect.LOGGER.debug("Tutorial nose equip handler initialized");
    }

    /**
     * Resets fired triggers for a player (e.g., on tutorial reset).
     * <p>
     * Keeps {@code lastEquippedNose} intact so that if the player is still
     * wearing a nose after reset, no false "change" is detected.
     * Only clears {@code firedTriggers} so triggers can fire again
     * when the player actually re-equips the nose.
     */
    public static void resetPlayer(UUID playerId) {
        // DO NOT clear lastEquippedNose — prevents false trigger when
        // player is still wearing a nose after reset
        firedTriggers.remove(playerId);
    }

    /**
     * Resets all fired triggers.
     */
    public static void resetAll() {
        // DO NOT clear lastEquippedNose — prevents false triggers
        firedTriggers.clear();
    }

    /**
     * Manually fires a nose equip trigger (for testing via command).
     * Bypasses the change detection and fired-check.
     */
    public static void manualTrigger(ServerPlayer player, ServerLevel level, String noseId) {
        Optional<TutorialNoseEquipTrigger.NoseEquipAction> triggerOpt =
                TutorialNoseEquipTrigger.getTrigger(level, noseId);
        if (triggerOpt.isEmpty()) {
            AromaAffect.LOGGER.warn("[NoseEquip] Manual trigger: no trigger for '{}'", noseId);
            return;
        }

        AromaAffect.LOGGER.info("[NoseEquip] Manual trigger fired for nose '{}' player '{}'",
                noseId, player.getName().getString());

        executeTriggerActions(player, level, triggerOpt.get());
    }

    public static void onPlayerLeave(UUID playerId) {
        // DO NOT clear lastEquippedNose — if the player reconnects still wearing
        // the same nose, we don't want a false "change" detection.
        // DO NOT clear firedTriggers — prevents re-firing on reconnect.
        // Both maps are cleaned up naturally when the player is no longer tracked.
    }

    private static void onServerTick(ServerLevel level) {
        if (ReplayCompat.isReplayServer(level.getServer())) return;
        if (!TutorialModule.isActive(level)) return;

        for (ServerPlayer player : level.players()) {
            checkNoseChange(player, level);
        }
    }

    private static void checkNoseChange(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        String currentNoseId = EquippedNoseHelper.getEquippedNoseId(player).orElse(null);
        String previousNoseId = lastEquippedNose.get(playerId);

        // Check if nose changed
        boolean changed;
        if (currentNoseId == null) {
            changed = previousNoseId != null;
        } else {
            changed = !currentNoseId.equals(previousNoseId);
        }

        if (!changed) return;

        // Update tracking
        if (currentNoseId != null) {
            lastEquippedNose.put(playerId, currentNoseId);
        } else {
            lastEquippedNose.remove(playerId);
        }

        AromaAffect.LOGGER.info("[NoseEquip] Player {} nose changed: '{}' -> '{}'",
                player.getName().getString(), previousNoseId, currentNoseId);

        // Only fire triggers when a nose is EQUIPPED (not unequipped)
        if (currentNoseId == null) return;

        // Check if there's a trigger for this nose
        Optional<TutorialNoseEquipTrigger.NoseEquipAction> triggerOpt =
                TutorialNoseEquipTrigger.getTrigger(level, currentNoseId);
        if (triggerOpt.isEmpty()) {
            AromaAffect.LOGGER.info("[NoseEquip] No trigger found for nose '{}'. Available triggers: {}",
                    currentNoseId, TutorialNoseEquipTrigger.getAllTriggerNoseIds(level));
            return;
        }

        // Check if already fired for this player
        Set<String> playerFired = firedTriggers.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        if (!playerFired.add(currentNoseId)) {
            AromaAffect.LOGGER.info("[NoseEquip] Trigger for '{}' already fired for player {}",
                    currentNoseId, player.getName().getString());
            return;
        }

        AromaAffect.LOGGER.info("[NoseEquip] FIRING trigger for nose '{}' player '{}'",
                currentNoseId, player.getName().getString());

        TutorialNoseEquipTrigger.NoseEquipAction action = triggerOpt.get();
        executeTriggerActions(player, level, action);
    }

    private static void executeTriggerActions(ServerPlayer player, ServerLevel level,
                                               TutorialNoseEquipTrigger.NoseEquipAction action) {
        AromaAffect.LOGGER.info("[NoseEquip] Executing actions - oliver:'{}' cinematic:'{}' waypoint:'{}' animation:'{}'",
                action.onCompleteOliverAction(), action.onCompleteCinematicId(),
                action.onCompleteWaypointId(), action.onCompleteAnimationId());

        // Oliver action
        if (action.hasOliverAction()) {
            executeOliverAction(player, level, action.onCompleteOliverAction());
        }

        // Cinematic
        if (action.hasCinematic()) {
            TutorialCinematicHandler.startCinematic(player, action.onCompleteCinematicId());
        }

        // Waypoint
        if (action.hasWaypoint()) {
            String waypointId = action.onCompleteWaypointId();
            Optional<TutorialWaypoint> wpOpt = TutorialWaypointManager.getWaypoint(level, waypointId);
            if (wpOpt.isPresent() && wpOpt.get().isComplete()) {
                TutorialWaypoint wp = wpOpt.get();
                TutorialWaypointAreaHandler.setActiveWaypoint(player.getUUID(), waypointId);
                TutorialWaypointNetworking.sendWaypointToPlayer(player, waypointId, wp.getValidPositions());
                AromaAffect.LOGGER.info("[NoseEquip] Activated waypoint '{}'", waypointId);
            } else {
                AromaAffect.LOGGER.warn("[NoseEquip] Waypoint '{}' not found or incomplete", waypointId);
            }
        }

        // Animation
        if (action.hasAnimation()) {
            boolean played = TutorialAnimationHandler.play(level, action.onCompleteAnimationId());
            AromaAffect.LOGGER.info("[NoseEquip] Animation '{}' play result: {}",
                    action.onCompleteAnimationId(), played);
        }
    }

    /**
     * Epic dimensional teleport with darkness, sounds, particles and screen flash.
     * Used for the Nether → End transition.
     */
    private static void performEpicTeleport(ServerPlayer player, ServerLevel level, int x, int y, int z) {
        double fromX = player.getX();
        double fromY = player.getY();
        double fromZ = player.getZ();

        // 1. Stop all current sounds
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(null, null));

        // 2. Departure effects — dramatic particles at origin
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                fromX, fromY + 1.0, fromZ, 80, 0.5, 1.0, 0.5, 0.15);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                fromX, fromY + 0.5, fromZ, 50, 1.0, 0.5, 1.0, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                fromX, fromY + 1.0, fromZ, 100, 0.8, 1.5, 0.8, 1.0);

        // 3. Departure sound — end portal
        level.playSound(null, fromX, fromY, fromZ,
                net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN,
                net.minecraft.sounds.SoundSource.AMBIENT, 1.0f, 0.8f);

        // 4. Apply Darkness effect (50 ticks = 2.5s) for dimensional transition feel
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DARKNESS, 50, 0, false, false, false));

        // 5. Send dream overlay flash (white screen)
        com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.8f);

        // 6. Teleport after a brief moment (10 ticks = 0.5s) for buildup
        scheduleDelayed(10, () -> {
            // Teleport
            player.teleportTo(level, x + 0.5, y, z + 0.5,
                    java.util.Set.of(), player.getYRot(), player.getXRot(), false);

            // Full white flash on arrival
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 1.0f);

            // Arrival particles — epic end dimension entrance
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    x + 0.5, y + 1.5, z + 0.5, 60, 1.5, 2.0, 1.5, 0.1);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                    x + 0.5, y + 1.0, z + 0.5, 100, 2.0, 1.0, 2.0, 0.2);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    x + 0.5, y + 0.5, z + 0.5, 40, 2.0, 0.5, 2.0, 0.5);

            // Arrival sound — ender dragon growl + amethyst chime
            level.playSound(null, x + 0.5, y, z + 0.5,
                    net.minecraft.sounds.SoundEvents.ENDER_DRAGON_AMBIENT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.6f, 0.5f);
            level.playSound(null, x + 0.5, y, z + 0.5,
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.AMBIENT, 1.0f, 0.5f);

            AromaAffect.LOGGER.debug("Epic teleport: player arrived at {}, {}, {}", x, y, z);
        });

        // 7. Fade out overlay gradually after arrival
        scheduleDelayed(25, () ->
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.5f));
        scheduleDelayed(35, () ->
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.2f));
        scheduleDelayed(45, () ->
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendClearOverlay(player));
    }

    private static void scheduleDelayed(int ticks, Runnable action) {
        synchronized (delayedActions) {
            delayedActions.add(new DelayedAction(ticks, action));
        }
    }

    private static void processDelayedActions() {
        synchronized (delayedActions) {
            Iterator<DelayedAction> it = delayedActions.iterator();
            while (it.hasNext()) {
                DelayedAction da = it.next();
                if (--da.ticksRemaining <= 0) {
                    da.action.run();
                    it.remove();
                }
            }
        }
    }

    private static class DelayedAction {
        int ticksRemaining;
        final Runnable action;
        DelayedAction(int ticks, Runnable action) {
            this.ticksRemaining = ticks;
            this.action = action;
        }
    }

    private static void executeOliverAction(ServerPlayer player, ServerLevel level, String action) {
        TutorialOliverEntity oliver = level.getEntitiesOfClass(TutorialOliverEntity.class,
                player.getBoundingBox().inflate(100.0), e -> true
        ).stream().findFirst().orElse(null);

        if (oliver == null) {
            AromaAffect.LOGGER.warn("Cannot execute Oliver action '{}': no Oliver found", action);
            return;
        }

        // Support multiple actions separated by semicolon
        String[] actions = action.split(";");
        String pendingDialogueId = null;

        for (String singleAction : actions) {
            singleAction = singleAction.trim();
            if (singleAction.isEmpty()) continue;

            String actionLower = singleAction.toLowerCase();

            if (actionLower.equals("follow")) {
                oliver.setFollowing(player);
            } else if (actionLower.equals("stop")) {
                oliver.setStationary();
            } else if (actionLower.startsWith("walkto:")) {
                String coordsStr = singleAction.substring(7);
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
                // Defer dialogue opening until after all other actions (especially trade:)
                pendingDialogueId = singleAction.substring(9).trim();
                oliver.setDialogueId(pendingDialogueId);
            } else if (actionLower.startsWith("trade:")) {
                oliver.setTradeId(singleAction.substring(6).trim());
                AromaAffect.LOGGER.debug("[NoseEquip] Oliver trade set to {}", singleAction.substring(6).trim());
            } else if (actionLower.equals("cleartrade")) {
                oliver.setTradeId("");
            } else if (actionLower.startsWith("teleportplayer:")) {
                String coordsStr = singleAction.substring(15);
                String[] parts = coordsStr.split(",");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());
                        performEpicTeleport(player, level, x, y, z);
                    } catch (NumberFormatException e) {
                        AromaAffect.LOGGER.warn("Invalid teleportplayer coordinates: {}", coordsStr);
                    }
                }
            }
        }

        // Open dialogue AFTER processing all actions (so trade is set first)
        if (pendingDialogueId != null) {
            AromaAffect.LOGGER.info("[NoseEquip] Opening dialogue '{}' (hasTrade: {}, tradeId: '{}')",
                    pendingDialogueId, oliver.hasTrade(), oliver.getTradeId());
            TutorialDialogueContentNetworking.sendOpenDialogue(
                    player, oliver.getId(), pendingDialogueId,
                    oliver.hasTrade(), oliver.getTradeId()
            );
        }
    }
}
