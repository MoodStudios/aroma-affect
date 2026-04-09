package com.ovrtechnology.tutorial.noseequip;

import com.ovrtechnology.AromaAffect;
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
        TickEvent.SERVER_POST.register(server -> processDelayedActions());
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
        AromaAffect.LOGGER.info("[NoseEquip] Executing actions - nose:'{}' oliver:'{}' cinematic:'{}' waypoint:'{}' animation:'{}'",
                action.noseId(), action.onCompleteOliverAction(), action.onCompleteCinematicId(),
                action.onCompleteWaypointId(), action.onCompleteAnimationId());

        // Auto clear inventory when equipping Prospector Nose (gold_nose)
        if ("gold_nose".equals(action.noseId())) {
            com.ovrtechnology.tutorial.trade.TutorialTradeHandler.clearInventoryKeeping(
                    player, java.util.Set.of("gold_nose"));
            AromaAffect.LOGGER.info("[NoseEquip] Auto-cleared inventory for Prospector Nose (gold_nose)");
        }

        // For blaze_nose (Dimensional Nose), ensure teleport uses epic sequence
        // Extract teleportplayer from oliver action, run it via performEpicTeleport(),
        // and pass remaining actions to executeOliverAction() without the teleport part
        if ("blaze_nose".equals(action.noseId()) && action.hasOliverAction()) {
            String oliverAction = action.onCompleteOliverAction();
            StringBuilder remainingActions = new StringBuilder();
            for (String part : oliverAction.split(";")) {
                part = part.trim();
                if (part.isEmpty()) continue;
                if (part.toLowerCase().startsWith("teleportplayer:")) {
                    String coordsStr = part.substring(15);
                    String[] coords = coordsStr.split(",");
                    if (coords.length == 3) {
                        try {
                            int x = Integer.parseInt(coords[0].trim());
                            int y = Integer.parseInt(coords[1].trim());
                            int z = Integer.parseInt(coords[2].trim());
                            AromaAffect.LOGGER.info("[NoseEquip] Epic teleport for blaze_nose to {},{},{}", x, y, z);
                            performEpicTeleport(player, level, x, y, z);
                        } catch (NumberFormatException e) {
                            AromaAffect.LOGGER.warn("[NoseEquip] Invalid blaze_nose teleport coords: {}", coordsStr);
                        }
                    }
                } else {
                    if (remainingActions.length() > 0) remainingActions.append(";");
                    remainingActions.append(part);
                }
            }
            // Execute remaining actions (dialogue, trade, etc.) without the teleport
            if (remainingActions.length() > 0) {
                executeOliverAction(player, level, remainingActions.toString());
            }
        } else if (action.hasOliverAction()) {
            // Normal oliver action processing for other noses
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
     * Extended buildup for dramatic effect. Public so other handlers can reuse it.
     */
    public static void performEpicTeleport(ServerPlayer player, ServerLevel level, int x, int y, int z) {
        double fromX = player.getX();
        double fromY = player.getY();
        double fromZ = player.getZ();

        // 1. Stop all current sounds
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(null, null));

        // 2. Initial buildup — ominous sounds
        level.playSound(null, fromX, fromY, fromZ,
                net.minecraft.sounds.SoundEvents.ENDER_EYE_DEATH,
                net.minecraft.sounds.SoundSource.AMBIENT, 1.0f, 0.3f);
        level.playSound(null, fromX, fromY, fromZ,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                net.minecraft.sounds.SoundSource.AMBIENT, 1.5f, 0.5f);

        // 3. Early overlay fade
        com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.3f);

        // 4. Apply Darkness effect (80 ticks = 4s) for extended dimensional transition
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DARKNESS, 80, 0, false, false, false));

        // 5. Departure particles — building up
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                fromX, fromY + 1.0, fromZ, 60, 0.5, 1.0, 0.5, 0.1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                fromX, fromY + 1.0, fromZ, 80, 0.8, 1.5, 0.8, 1.0);

        // 6. Intensify at tick 10
        scheduleDelayed(10, () -> {
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.6f);
            level.playSound(null, fromX, fromY, fromZ,
                    net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN,
                    net.minecraft.sounds.SoundSource.AMBIENT, 1.0f, 0.8f);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    fromX, fromY + 0.5, fromZ, 60, 1.0, 0.5, 1.0, 0.05);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                    fromX, fromY + 1.0, fromZ, 30, 0.5, 1.0, 0.5, 0.1);
        });

        // 7. Flash to white at tick 18
        scheduleDelayed(18, () ->
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.9f));

        // 8. Teleport at tick 22 (1.1s buildup)
        scheduleDelayed(22, () -> {
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 1.0f);

            player.teleportTo(level, x + 0.5, y, z + 0.5,
                    java.util.Set.of(), player.getYRot(), player.getXRot(), false);

            // Arrival particles — epic end dimension entrance
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    x + 0.5, y + 1.5, z + 0.5, 80, 2.0, 2.5, 2.0, 0.1);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                    x + 0.5, y + 1.0, z + 0.5, 120, 2.0, 1.5, 2.0, 0.2);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    x + 0.5, y + 0.5, z + 0.5, 50, 2.0, 0.5, 2.0, 0.5);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x + 0.5, y + 0.2, z + 0.5, 15, 1.5, 0.1, 1.5, 0.02);

            // Arrival sounds
            level.playSound(null, x + 0.5, y, z + 0.5,
                    net.minecraft.sounds.SoundEvents.ENDER_DRAGON_AMBIENT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.6f, 0.5f);
            level.playSound(null, x + 0.5, y, z + 0.5,
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.AMBIENT, 1.0f, 0.5f);
            level.playSound(null, x + 0.5, y, z + 0.5,
                    net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    net.minecraft.sounds.SoundSource.AMBIENT, 0.8f, 0.6f);

            AromaAffect.LOGGER.debug("Epic teleport: player arrived at {}, {}, {}", x, y, z);
        });

        // 9. Gradual fade out
        scheduleDelayed(35, () ->
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.7f));
        scheduleDelayed(42, () ->
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.4f));
        scheduleDelayed(50, () ->
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.2f));
        scheduleDelayed(60, () ->
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendOverlayProgress(player, 0.05f));
        scheduleDelayed(70, () ->
            com.ovrtechnology.network.TutorialDreamOverlayNetworking.sendClearOverlay(player));
    }

    public static void scheduleDelayed(int ticks, Runnable action) {
        synchronized (delayedActions) {
            delayedActions.add(new DelayedAction(ticks, action));
        }
    }

    private static void processDelayedActions() {
        List<Runnable> toRun = new ArrayList<>();
        synchronized (delayedActions) {
            Iterator<DelayedAction> it = delayedActions.iterator();
            while (it.hasNext()) {
                DelayedAction da = it.next();
                if (--da.ticksRemaining <= 0) {
                    toRun.add(da.action);
                    it.remove();
                }
            }
        }
        // Execute outside synchronized block to avoid ConcurrentModificationException
        // when callbacks schedule new delayed actions
        for (Runnable r : toRun) {
            r.run();
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
        // Support multiple actions separated by semicolon
        String[] actions = action.split(";");

        // First pass: execute player-only actions (don't need Oliver)
        for (String singleAction : actions) {
            singleAction = singleAction.trim();
            if (singleAction.isEmpty()) continue;
            String actionLower = singleAction.toLowerCase();

            if (com.ovrtechnology.tutorial.OliverActionHelper.processPlayerAction(player, singleAction)) {
                continue;
            } else if (actionLower.startsWith("clearinventory:")) {
                String keepStr = singleAction.substring(15);
                java.util.Set<String> keepItems = new java.util.HashSet<>(java.util.Arrays.asList(keepStr.split(",")));
                com.ovrtechnology.tutorial.trade.TutorialTradeHandler.clearInventoryKeeping(player, keepItems);
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

        // Second pass: execute Oliver-dependent actions
        TutorialOliverEntity oliver = level.getEntitiesOfClass(TutorialOliverEntity.class,
                player.getBoundingBox().inflate(100.0), e -> true
        ).stream().findFirst().orElse(null);

        if (oliver == null) {
            AromaAffect.LOGGER.warn("Cannot execute Oliver action '{}': no Oliver found", action);
            return;
        }

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
                pendingDialogueId = singleAction.substring(9).trim();
                oliver.setDialogueId(pendingDialogueId);
            } else if (actionLower.startsWith("trade:")) {
                oliver.setTradeId(singleAction.substring(6).trim());
                AromaAffect.LOGGER.debug("[NoseEquip] Oliver trade set to {}", singleAction.substring(6).trim());
            } else if (actionLower.equals("cleartrade")) {
                oliver.setTradeId("");
            }
            // clearinventory and teleportplayer already handled in first pass
        }

        // Open dialogue AFTER processing all actions (so trade is set first)
        if (pendingDialogueId != null) {
            AromaAffect.LOGGER.info("[NoseEquip] Opening dialogue '{}' (hasTrade: {}, tradeId: '{}')",
                    pendingDialogueId, oliver.hasTrade(), oliver.getTradeId());
            TutorialDialogueContentNetworking.sendOpenDialogue(
                    player, oliver.getId(), pendingDialogueId,
                    oliver.hasTrade(), oliver.getTradeId(), true
            );
        }
    }
}
