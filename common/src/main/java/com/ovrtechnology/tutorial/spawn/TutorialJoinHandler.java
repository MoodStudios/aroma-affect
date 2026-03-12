package com.ovrtechnology.tutorial.spawn;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialAnimationNetworking;
import com.ovrtechnology.network.TutorialChestNetworking;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialIntroNetworking;
import com.ovrtechnology.network.TutorialPortalOverlayNetworking;
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
            // Reset scent counter client state (critical for singleplayer where JVM is shared)
            com.ovrtechnology.network.TutorialScentCounterNetworking.deactivateClient();
            // Reset finish zone trigger tracking
            com.ovrtechnology.tutorial.finishscreen.TutorialFinishZoneHandler.resetPlayer(player.getUUID());
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
        // Ensure scent counter starts deactivated on reconnect
        com.ovrtechnology.network.TutorialScentCounterNetworking.sendDeactivate(player);
        com.ovrtechnology.tutorial.finishscreen.TutorialFinishZoneHandler.resetPlayer(player.getUUID());

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

        // Reset player progress tracking
        processedPlayers.remove(playerId);
        TutorialWaypointAreaHandler.resetPlayer(playerId);
        TutorialNoseEquipHandler.resetPlayer(playerId);
        TutorialBossAreaManager.get(level).resetPlayerTriggers(playerId);
        TutorialBossAreaHandler.clearAllBosses();
        TutorialPopupZoneHandler.clearPlayer(playerId);
        com.ovrtechnology.network.TutorialScentCounterNetworking.sendDeactivate(player);
        com.ovrtechnology.tutorial.finishscreen.TutorialFinishZoneHandler.resetPlayer(playerId);

        // Clear client-side state
        TutorialWaypointNetworking.sendClearToPlayer(player);
        TutorialChestNetworking.syncAllChestsToPlayer(player);
        TutorialAnimationNetworking.sendAnimationReset(player);
        TutorialDialogueContentNetworking.syncToPlayer(player, level);

        // Teleport to spawn FIRST (so chunks around spawn load)
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

        // Reset entities AFTER teleport (chunks around spawn now loaded)
        resetAllEntities(level);
        TutorialNoseEquipHandler.scheduleDelayed(10, () -> resetAllEntities(level));

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

        // Reset player progress tracking
        processedPlayers.remove(playerId);
        TutorialWaypointAreaHandler.resetPlayer(playerId);
        TutorialNoseEquipHandler.resetPlayer(playerId);
        TutorialBossAreaManager.get(level).resetPlayerTriggers(playerId);
        TutorialBossAreaHandler.clearAllBosses();
        TutorialPopupZoneHandler.clearPlayer(playerId);
        com.ovrtechnology.network.TutorialScentCounterNetworking.sendDeactivate(player);
        com.ovrtechnology.tutorial.finishscreen.TutorialFinishZoneHandler.resetPlayer(playerId);

        // Clear client-side state
        TutorialWaypointNetworking.sendClearToPlayer(player);
        TutorialChestNetworking.syncAllChestsToPlayer(player);
        TutorialAnimationNetworking.sendAnimationReset(player);
        TutorialDialogueContentNetworking.syncToPlayer(player, level);

        // Determine walkaround spawn (or fallback to normal spawn)
        TutorialSpawnManager.SpawnData walkaroundSpawn = null;
        var walkaroundOpt = TutorialSpawnManager.getWalkaroundSpawn(level);
        if (walkaroundOpt.isPresent()) {
            walkaroundSpawn = walkaroundOpt.get();
        } else {
            var spawnOpt = TutorialSpawnManager.getSpawn(level);
            if (spawnOpt.isPresent()) {
                walkaroundSpawn = spawnOpt.get();
            }
            AromaAffect.LOGGER.warn("No walkaround spawn set - using normal spawn. Set with /tutorial setwalkaround");
        }

        if (walkaroundSpawn != null) {
            performWalkaroundTeleport(player, level, walkaroundSpawn);
        }

        // No intro sequence or waypoints for walkaround - just free exploration
    }

    /**
     * Removes a player from the intro state (e.g., when stopped via command).
     */
    /**
     * Performs an animated walkaround teleport with overlay fade, sounds, and particles.
     */
    private static void performWalkaroundTeleport(ServerPlayer player, ServerLevel level, TutorialSpawnManager.SpawnData spawn) {
        // Phase 1: Buildup - fade to full overlay with sounds
        TutorialPortalOverlayNetworking.sendOverlayProgress(player, 0.5f);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 1.0f, 0.8f);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.AMBIENT, 0.5f, 0.6f);

        // Departure particles
        level.sendParticles(ParticleTypes.CHERRY_LEAVES,
                player.getX(), player.getY() + 1.0, player.getZ(),
                30, 1.0, 1.0, 1.0, 0.02);

        // Phase 2: Full overlay
        TutorialNoseEquipHandler.scheduleDelayed(8, () -> {
            TutorialPortalOverlayNetworking.sendOverlayProgress(player, 0.85f);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 0.4f, 1.2f);
        });

        // Phase 3: Teleport while fully covered
        TutorialNoseEquipHandler.scheduleDelayed(15, () -> {
            TutorialPortalOverlayNetworking.sendOverlayProgress(player, 1.0f);
        });

        TutorialNoseEquipHandler.scheduleDelayed(20, () -> {
            // Actual teleport
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

            // Reset entities AFTER teleport (chunks around spawn now loaded)
            resetAllEntities(level);
            TutorialNoseEquipHandler.scheduleDelayed(10, () -> resetAllEntities(level));

            // Arrival sound
            level.playSound(null, spawn.pos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 1.0f, 1.2f);
        });

        // Phase 4: Arrival particles
        for (int tick = 25; tick <= 45; tick += 5) {
            TutorialNoseEquipHandler.scheduleDelayed(tick, () -> {
                double px = spawn.pos().getX() + 0.5;
                double py = spawn.pos().getY() + 0.5;
                double pz = spawn.pos().getZ() + 0.5;
                level.sendParticles(ParticleTypes.CHERRY_LEAVES, px, py, pz,
                        15, 1.5, 1.2, 1.5, 0.01);
                level.sendParticles(ParticleTypes.END_ROD, px, py + 1.0, pz,
                        5, 0.5, 0.5, 0.5, 0.02);
            });
        }

        // Phase 5: Gradual fade-out
        TutorialNoseEquipHandler.scheduleDelayed(30, () ->
                TutorialPortalOverlayNetworking.sendOverlayProgress(player, 0.7f));
        TutorialNoseEquipHandler.scheduleDelayed(38, () ->
                TutorialPortalOverlayNetworking.sendOverlayProgress(player, 0.5f));
        TutorialNoseEquipHandler.scheduleDelayed(46, () ->
                TutorialPortalOverlayNetworking.sendOverlayProgress(player, 0.3f));
        TutorialNoseEquipHandler.scheduleDelayed(54, () ->
                TutorialPortalOverlayNetworking.sendOverlayProgress(player, 0.15f));
        TutorialNoseEquipHandler.scheduleDelayed(62, () ->
                TutorialPortalOverlayNetworking.sendOverlayProgress(player, 0.05f));
        TutorialNoseEquipHandler.scheduleDelayed(70, () -> {
                TutorialPortalOverlayNetworking.sendClearOverlay(player);
                // Activate scent counter HUD for walkaround mode
                com.ovrtechnology.network.TutorialScentCounterNetworking.sendActivate(player, 16);
                AromaAffect.LOGGER.info("Scent counter activated for walkaround player {}", player.getName().getString());
        });
    }

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

        // 4. Reset all player-specific progress tracking
        UUID playerId = player.getUUID();
        processedPlayers.remove(playerId);
        playersInIntro.remove(playerId);
        TutorialWaypointAreaHandler.resetPlayer(playerId);
        TutorialNoseEquipHandler.resetPlayer(playerId);
        TutorialBossAreaManager.get(level).resetPlayerTriggers(playerId);
        TutorialBossAreaHandler.clearAllBosses();
        TutorialPopupZoneHandler.clearPlayer(playerId);
        com.ovrtechnology.network.TutorialScentCounterNetworking.sendDeactivate(player);
        com.ovrtechnology.network.TutorialScentCounterNetworking.deactivateClient();
        com.ovrtechnology.tutorial.finishscreen.TutorialFinishZoneHandler.resetPlayer(playerId);

        // 5. Clear client-side state
        TutorialWaypointNetworking.sendClearToPlayer(player);
        TutorialChestNetworking.syncAllChestsToPlayer(player);
        TutorialAnimationNetworking.sendAnimationReset(player);
        TutorialDialogueContentNetworking.syncToPlayer(player, level);

        // 6. Check for intro cinematic
        var introCinematicOpt = TutorialSpawnManager.getIntroCinematicId(level);
        if (introCinematicOpt.isPresent()) {
            playersInIntro.add(playerId);
            TutorialCinematicHandler.startCinematic(player, introCinematicOpt.get(), true);
            TutorialIntroNetworking.sendOpenIntro(player);
            AromaAffect.LOGGER.info("Auto-reset complete - player {} entering intro cinematic", player.getName().getString());
            // Entity reset will happen in handlePlayDemo/handleWalkaround after teleport
            return;
        }

        // 7. Teleport to spawn point FIRST (so chunks around spawn load)
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

        // 8. Reset entities AFTER teleport (chunks around spawn are now loaded)
        resetAllEntities(level);

        // 9. Also schedule a delayed re-reset to catch entities in chunks that load after
        TutorialNoseEquipHandler.scheduleDelayed(10, () -> resetAllEntities(level));

        // 10. Play intro sequence and activate first waypoint
        playIntroSequence(player, level);
        activateFirstWaypoint(player, level);

        AromaAffect.LOGGER.info("Auto-reset complete for player {} - tutorial restarted", player.getName().getString());
    }

    /**
     * Resets all Oliver and NoseSmith entities in the level to their home positions.
     * <p>
     * Uses a two-step approach: first iterates all loaded entities, then specifically
     * searches near the NoseSmith's configured spawn position (force-loading the chunk
     * if necessary) to catch entities that may be in unloaded chunks.
     */
    private static void resetAllEntities(ServerLevel level) {
        // Step 1: Reset all Oliver entities found in loaded chunks
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof TutorialOliverEntity oliver) {
                oliver.resetToHome();
            }
        }

        // Step 2: Reset NoseSmith by searching near its configured spawn position
        // This ensures we find it even if its chunk wasn't loaded yet
        resetNoseSmith(level);
    }

    /**
     * Finds and resets the NoseSmith entity near its configured spawn position.
     * Force-loads the chunk at the spawn position to ensure the entity is accessible.
     */
    private static void resetNoseSmith(ServerLevel level) {
        var spawnOpt = com.ovrtechnology.tutorial.nosesmith.TutorialNoseSmithManager.getSpawnPos(level);
        if (spawnOpt.isEmpty()) {
            // No spawn configured, fall back to searching all loaded entities
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof com.ovrtechnology.entity.nosesmith.NoseSmithEntity noseSmith) {
                    noseSmith.resetQuest();
                }
            }
            return;
        }

        net.minecraft.core.BlockPos spawnPos = spawnOpt.get();
        float spawnYaw = com.ovrtechnology.tutorial.nosesmith.TutorialNoseSmithManager.getSpawnYaw(level);

        // Force-load the chunk at spawn position so the entity is accessible
        level.getChunk(spawnPos);

        // Search in a wide area around the spawn position (NoseSmith may have wandered)
        net.minecraft.world.phys.AABB searchArea = new net.minecraft.world.phys.AABB(
                spawnPos.getX() - 200, spawnPos.getY() - 100, spawnPos.getZ() - 200,
                spawnPos.getX() + 200, spawnPos.getY() + 100, spawnPos.getZ() + 200
        );

        java.util.List<com.ovrtechnology.entity.nosesmith.NoseSmithEntity> noseSmiths =
                level.getEntitiesOfClass(com.ovrtechnology.entity.nosesmith.NoseSmithEntity.class, searchArea);

        if (noseSmiths.isEmpty()) {
            // Also try all loaded entities as fallback
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof com.ovrtechnology.entity.nosesmith.NoseSmithEntity noseSmith) {
                    noseSmith.resetQuest();
                    noseSmith.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    noseSmith.setYRot(spawnYaw);
                    noseSmith.setYHeadRot(spawnYaw);
                    noseSmith.setYBodyRot(spawnYaw);
                    AromaAffect.LOGGER.info("Reset NoseSmith (fallback search) and teleported to {}", spawnPos);
                }
            }
            return;
        }

        for (com.ovrtechnology.entity.nosesmith.NoseSmithEntity noseSmith : noseSmiths) {
            noseSmith.resetQuest();
            noseSmith.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            noseSmith.setYRot(spawnYaw);
            noseSmith.setYHeadRot(spawnYaw);
            noseSmith.setYBodyRot(spawnYaw);
            AromaAffect.LOGGER.info("Reset NoseSmith and teleported to {}", spawnPos);
        }
    }
}
