package com.ovrtechnology.tutorial.spawn;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialAnimationNetworking;
import com.ovrtechnology.network.TutorialChestNetworking;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialIntroNetworking;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.animation.TutorialAnimationManager;
import com.ovrtechnology.tutorial.boss.TutorialBossAreaHandler;
import com.ovrtechnology.tutorial.boss.TutorialBossAreaManager;
import com.ovrtechnology.tutorial.chest.TutorialChestManager;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.noseequip.TutorialNoseEquipHandler;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import com.ovrtechnology.tutorial.popupzone.TutorialPopupZoneHandler;
import com.ovrtechnology.tutorial.portal.TutorialNetherPortalBlocker;
import com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaManager;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles player join events for tutorial worlds.
 * <p>
 * When a player joins a world with the tutorial GameRule enabled and a
 * spawn point set, they are teleported to the spawn with a cinematic
 * intro sequence including:
 * <ul>
 *   <li>Particles (cherry leaves + end rod sparkles)</li>
 *   <li>Ambient sound (amethyst chime)</li>
 *   <li>Title: "OVR EXPERIENCE" (purple, bold)</li>
 *   <li>Subtitle: "Discovery unlocked..." (white, italic)</li>
 * </ul>
 */
public final class TutorialJoinHandler {

    /**
     * Thread-safe set of players who have already received the intro sequence.
     * Prevents duplicate triggers on dimension changes or reconnects.
     */
    private static final Set<UUID> processedPlayers = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> playersInIntro = ConcurrentHashMap.newKeySet();

    private TutorialJoinHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Initializes the join handler.
     * <p>
     * Registers event listeners for player join/quit events.
     * Should be called during tutorial module initialization.
     */
    public static void init() {
        PlayerEvent.PLAYER_JOIN.register(TutorialJoinHandler::onPlayerJoin);
        PlayerEvent.PLAYER_QUIT.register(player -> {
            processedPlayers.remove(player.getUUID());
            playersInIntro.remove(player.getUUID());
            TutorialNetherPortalBlocker.onPlayerLeave(player.getUUID());
            TutorialNoseEquipHandler.onPlayerLeave(player.getUUID());
        });

        AromaAffect.LOGGER.debug("Tutorial join handler initialized");
    }

    private static void onPlayerJoin(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        // Check if tutorial mode is active
        if (!TutorialModule.isActive(level)) {
            return;
        }

        // Check if spawn point is set
        var spawnOpt = TutorialSpawnManager.getSpawn(level);
        if (spawnOpt.isEmpty()) {
            AromaAffect.LOGGER.warn("Tutorial is active but no spawn point configured! Use /tutorial setspawn");
            return;
        }

        // SINGLEPLAYER OPTIMIZATION: Auto-reset everything when player joins
        // This ensures a fresh tutorial experience every time the player enters
        boolean isSingleplayer = level.getServer().isSingleplayer();

        if (isSingleplayer) {
            AromaAffect.LOGGER.info("Singleplayer detected - performing auto-reset for tutorial");
            performFullReset(player, level);
            return;
        }

        // MULTIPLAYER FLOW (original logic)
        // Clean up any previous state
        if (playersInIntro.contains(player.getUUID())) {
            playersInIntro.remove(player.getUUID());
            TutorialCinematicHandler.stopCinematic(player);
            processedPlayers.remove(player.getUUID());
        }

        // Only show intro sequence once per session, but ALWAYS teleport to spawn
        boolean isFirstJoin = processedPlayers.add(player.getUUID());

        TutorialSpawnManager.SpawnData spawn = spawnOpt.get();

        // Check if there's an intro cinematic configured (only for first join)
        var introCinematicOpt = TutorialSpawnManager.getIntroCinematicId(level);
        if (isFirstJoin && introCinematicOpt.isPresent()) {
            // Intro cinematic flow: start looping cinematic + open intro screen
            playersInIntro.add(player.getUUID());
            TutorialCinematicHandler.startCinematic(player, introCinematicOpt.get(), true);
            TutorialDialogueContentNetworking.syncToPlayer(player, level);
            TutorialIntroNetworking.sendOpenIntro(player);
            AromaAffect.LOGGER.debug("Player {} entered intro cinematic", player.getName().getString());
            return;
        }

        // ALWAYS teleport to tutorial spawn point when tutorial is active
        player.teleportTo(
                level,
                spawn.pos().getX() + 0.5,
                spawn.pos().getY(),
                spawn.pos().getZ() + 0.5,
                Set.of(),
                spawn.yaw(),
                spawn.pitch(),
                false
        );

        // Sync custom dialogue texts to client
        TutorialDialogueContentNetworking.syncToPlayer(player, level);

        // Only play intro sequence on first join
        if (isFirstJoin) {
            playIntroSequence(player, level);
            activateFirstWaypoint(player, level);
        }

        AromaAffect.LOGGER.info("Player {} teleported to tutorial spawn (firstJoin={})",
                player.getName().getString(), isFirstJoin);
    }

    private static void activateFirstWaypoint(ServerPlayer player, ServerLevel level) {
        var firstWpOpt = TutorialSpawnManager.getFirstWaypointId(level);
        if (firstWpOpt.isEmpty()) {
            return;
        }

        String waypointId = firstWpOpt.get();
        var wpOpt = TutorialWaypointManager.getWaypoint(level, waypointId);
        if (wpOpt.isPresent() && wpOpt.get().isComplete()) {
            TutorialWaypoint wp = wpOpt.get();
            TutorialWaypointAreaHandler.setActiveWaypoint(player.getUUID(), waypointId);
            TutorialWaypointNetworking.sendWaypointToPlayer(player, waypointId, wp.getValidPositions());
            AromaAffect.LOGGER.debug("Activated first waypoint '{}' for player {}", waypointId, player.getName().getString());
        } else {
            AromaAffect.LOGGER.warn("First waypoint '{}' not found or incomplete", waypointId);
        }
    }

    private static void playIntroSequence(ServerPlayer player, ServerLevel level) {
        // Title animation timings (in ticks): fade in, stay, fade out
        final int fadeIn = 20;   // 1 second
        final int stay = 60;     // 3 seconds
        final int fadeOut = 20;  // 1 second

        // Send title animation timing packet
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));

        // Title: "OVR EXPERIENCE" in OVR purple (#A890F0), bold
        Component title = Component.literal("OVR EXPERIENCE")
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(0xA890F0))
                        .withBold(true));
        player.connection.send(new ClientboundSetTitleTextPacket(title));

        // Subtitle: "Discovery unlocked..." in white, italic
        Component subtitle = Component.literal("Discovery unlocked...")
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(0xFFFFFF))
                        .withItalic(true));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));

        // Play ambient sound
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 1.0f, 0.8f);

        // Spawn particles around the player
        spawnIntroParticles(player, level);
    }

    private static void spawnIntroParticles(ServerPlayer player, ServerLevel level) {
        Vec3 pos = player.position();
        RandomSource random = level.getRandom();

        // Cherry blossom particles in a circle around the player
        final int particleCount = 30;
        for (int i = 0; i < particleCount; i++) {
            double angle = (2.0 * Math.PI * i) / particleCount;
            double radius = 2.0 + (random.nextDouble() * 1.5);
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;
            double y = pos.y + 0.5 + (random.nextDouble() * 2.0);

            level.sendParticles(
                    ParticleTypes.CHERRY_LEAVES,
                    x, y, z,
                    3,      // count
                    0.2,    // xSpread
                    0.3,    // ySpread
                    0.2,    // zSpread
                    0.02    // speed
            );
        }

        // Central sparkle effect (end rod particles)
        level.sendParticles(
                ParticleTypes.END_ROD,
                pos.x, pos.y + 1.0, pos.z,
                15,     // count
                0.5,    // xSpread
                0.8,    // ySpread
                0.5,    // zSpread
                0.05    // speed
        );
    }

    /**
     * Called when a player clicks PLAY DEMO on the intro screen.
     * Performs full reset, teleports to spawn, plays intro sequence, activates first waypoint.
     */
    public static void handlePlayDemo(ServerPlayer player) {
        if (!playersInIntro.remove(player.getUUID())) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();

        // Stop the looping cinematic
        TutorialCinematicHandler.stopCinematic(player);

        // FULL RESET: Reset all world and player state
        // Re-enable map protection
        com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaHandler.disableBypass();

        // Reset world state
        TutorialChestManager.resetAllChests(level);
        TutorialAnimationManager.resetAllAnimations(level);
        TutorialRegenAreaManager.restoreAllAreas(level);

        // Reset player inventory and health
        player.getInventory().clearContent();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        player.setGameMode(GameType.SURVIVAL);

        // Reset all entities (Oliver, NoseSmith)
        resetAllEntities(level);

        // Reset player progress tracking
        processedPlayers.remove(playerId);
        TutorialWaypointAreaHandler.resetPlayer(playerId);
        TutorialNoseEquipHandler.resetPlayer(playerId);
        TutorialBossAreaManager.get(level).resetPlayerTriggers(playerId);
        TutorialBossAreaHandler.clearAllBosses();
        TutorialPopupZoneHandler.clearPlayer(playerId);

        // Clear client-side state
        TutorialWaypointNetworking.sendClearToPlayer(player);
        TutorialChestNetworking.syncAllChestsToPlayer(player);
        TutorialAnimationNetworking.sendAnimationReset(player);
        TutorialDialogueContentNetworking.syncToPlayer(player, level);

        // Teleport to spawn
        var spawnOpt = TutorialSpawnManager.getSpawn(level);
        if (spawnOpt.isPresent()) {
            TutorialSpawnManager.SpawnData spawn = spawnOpt.get();
            player.teleportTo(
                    level,
                    spawn.pos().getX() + 0.5,
                    spawn.pos().getY(),
                    spawn.pos().getZ() + 0.5,
                    Set.of(),
                    spawn.yaw(),
                    spawn.pitch(),
                    false
            );
        }

        // Play the intro sequence (titles, particles, sound)
        playIntroSequence(player, level);

        // Activate first waypoint
        activateFirstWaypoint(player, level);

        AromaAffect.LOGGER.info("Player {} started tutorial (PLAY DEMO) - full reset performed", player.getName().getString());
    }

    /**
     * Called when a player clicks WALKAROUND on the intro screen.
     * Performs full reset, teleports to walkaround spawn for free exploration.
     */
    public static void handleWalkaround(ServerPlayer player) {
        if (!playersInIntro.remove(player.getUUID())) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();

        // Stop the looping cinematic
        TutorialCinematicHandler.stopCinematic(player);

        // FULL RESET: Reset all world and player state
        // Re-enable map protection
        com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaHandler.disableBypass();

        // Reset world state
        TutorialChestManager.resetAllChests(level);
        TutorialAnimationManager.resetAllAnimations(level);
        TutorialRegenAreaManager.restoreAllAreas(level);

        // Reset player inventory and health
        player.getInventory().clearContent();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        player.setGameMode(GameType.SURVIVAL);

        // Reset all entities (Oliver, NoseSmith)
        resetAllEntities(level);

        // Reset player progress tracking
        processedPlayers.remove(playerId);
        TutorialWaypointAreaHandler.resetPlayer(playerId);
        TutorialNoseEquipHandler.resetPlayer(playerId);
        TutorialBossAreaManager.get(level).resetPlayerTriggers(playerId);
        TutorialBossAreaHandler.clearAllBosses();
        TutorialPopupZoneHandler.clearPlayer(playerId);

        // Clear client-side state
        TutorialWaypointNetworking.sendClearToPlayer(player);
        TutorialChestNetworking.syncAllChestsToPlayer(player);
        TutorialAnimationNetworking.sendAnimationReset(player);
        TutorialDialogueContentNetworking.syncToPlayer(player, level);

        // Teleport to walkaround spawn (if set), otherwise use normal spawn
        var walkaroundOpt = TutorialSpawnManager.getWalkaroundSpawn(level);
        if (walkaroundOpt.isPresent()) {
            TutorialSpawnManager.SpawnData spawn = walkaroundOpt.get();
            player.teleportTo(
                    level,
                    spawn.pos().getX() + 0.5,
                    spawn.pos().getY(),
                    spawn.pos().getZ() + 0.5,
                    Set.of(),
                    spawn.yaw(),
                    spawn.pitch(),
                    false
            );
            AromaAffect.LOGGER.info("Player {} entered WALKAROUND mode at {} - full reset performed", player.getName().getString(), spawn.pos());
        } else {
            // Fallback to normal spawn if walkaround not configured
            var spawnOpt = TutorialSpawnManager.getSpawn(level);
            if (spawnOpt.isPresent()) {
                TutorialSpawnManager.SpawnData spawn = spawnOpt.get();
                player.teleportTo(
                        level,
                        spawn.pos().getX() + 0.5,
                        spawn.pos().getY(),
                        spawn.pos().getZ() + 0.5,
                        Set.of(),
                        spawn.yaw(),
                        spawn.pitch(),
                        false
                );
            }
            AromaAffect.LOGGER.warn("No walkaround spawn set - using normal spawn. Set with /tutorial setwalkaround");
        }

        // No intro sequence or waypoints for walkaround - just free exploration
    }

    /**
     * Removes a player from the intro state (e.g., when stopped via command).
     */
    public static void removeFromIntro(UUID playerId) {
        playersInIntro.remove(playerId);
    }

    /**
     * Resets the processed state for a specific player.
     * <p>
     * Call this to allow the intro sequence to play again for a player
     * (e.g., for testing or after a tutorial reset).
     *
     * @param playerId the player's UUID
     */
    public static void resetPlayer(UUID playerId) {
        processedPlayers.remove(playerId);
    }

    /**
     * Resets all processed player states.
     * <p>
     * Call this to allow the intro sequence to play again for all players.
     */
    public static void resetAll() {
        processedPlayers.clear();
    }

    /**
     * Performs a FULL tutorial reset for singleplayer.
     * This resets all world state and player progress to give a fresh experience.
     */
    private static void performFullReset(ServerPlayer player, ServerLevel level) {
        // 0. Re-enable map protection (in case it was bypassed)
        com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaHandler.disableBypass();

        // 1. Reset all world state (chests, animations, regen areas)
        TutorialChestManager.resetAllChests(level);
        TutorialAnimationManager.resetAllAnimations(level);
        TutorialRegenAreaManager.restoreAllAreas(level);

        // 2. Clear player inventory and restore health/hunger
        player.getInventory().clearContent();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);

        // 3. Stop any active cinematic and force survival mode
        TutorialCinematicHandler.stopCinematic(player);
        player.setGameMode(GameType.SURVIVAL);

        // 4. Reset all Oliver and NoseSmith entities to home positions
        resetAllEntities(level);

        // 5. Reset all player-specific progress tracking
        UUID playerId = player.getUUID();
        processedPlayers.remove(playerId);
        playersInIntro.remove(playerId);
        TutorialWaypointAreaHandler.resetPlayer(playerId);
        TutorialNoseEquipHandler.resetPlayer(playerId);
        TutorialBossAreaManager.get(level).resetPlayerTriggers(playerId);
        TutorialBossAreaHandler.clearAllBosses();
        TutorialPopupZoneHandler.clearPlayer(playerId);

        // 6. Clear client-side state
        TutorialWaypointNetworking.sendClearToPlayer(player);
        TutorialChestNetworking.syncAllChestsToPlayer(player);
        TutorialAnimationNetworking.sendAnimationReset(player);
        TutorialDialogueContentNetworking.syncToPlayer(player, level);

        // 7. Check for intro cinematic
        var introCinematicOpt = TutorialSpawnManager.getIntroCinematicId(level);
        if (introCinematicOpt.isPresent()) {
            playersInIntro.add(playerId);
            TutorialCinematicHandler.startCinematic(player, introCinematicOpt.get(), true);
            TutorialIntroNetworking.sendOpenIntro(player);
            AromaAffect.LOGGER.info("Auto-reset complete - player {} entering intro cinematic", player.getName().getString());
            return;
        }

        // 8. Teleport to spawn point
        var spawnOpt = TutorialSpawnManager.getSpawn(level);
        if (spawnOpt.isPresent()) {
            TutorialSpawnManager.SpawnData spawn = spawnOpt.get();
            player.teleportTo(
                    level,
                    spawn.pos().getX() + 0.5,
                    spawn.pos().getY(),
                    spawn.pos().getZ() + 0.5,
                    Set.of(),
                    spawn.yaw(),
                    spawn.pitch(),
                    false
            );
        }

        // 9. Play intro sequence and activate first waypoint
        playIntroSequence(player, level);
        activateFirstWaypoint(player, level);

        AromaAffect.LOGGER.info("Auto-reset complete for player {} - tutorial restarted", player.getName().getString());
    }

    /**
     * Resets all Oliver and NoseSmith entities in the level to their home positions.
     */
    private static void resetAllEntities(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof TutorialOliverEntity oliver) {
                oliver.resetToHome();
            }
            if (entity instanceof com.ovrtechnology.entity.nosesmith.NoseSmithEntity noseSmith) {
                noseSmith.resetQuest();
                com.ovrtechnology.tutorial.nosesmith.TutorialNoseSmithManager.getSpawnPos(level).ifPresent(pos -> {
                    float yaw = com.ovrtechnology.tutorial.nosesmith.TutorialNoseSmithManager.getSpawnYaw(level);
                    noseSmith.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    noseSmith.setYRot(yaw);
                    noseSmith.setYHeadRot(yaw);
                    noseSmith.setYBodyRot(yaw);
                });
            }
        }
    }
}
