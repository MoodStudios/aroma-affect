package com.ovrtechnology.tutorial.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ovrtechnology.network.TutorialAnimationNetworking;
import com.ovrtechnology.network.TutorialChestNetworking;
import com.ovrtechnology.network.TutorialDialogueContentNetworking;
import com.ovrtechnology.network.TutorialWaypointNetworking;
import com.ovrtechnology.tutorial.animation.TutorialAnimationManager;
import com.ovrtechnology.tutorial.chest.TutorialChestManager;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import com.ovrtechnology.tutorial.command.TutorialSubCommand;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaManager;
import com.ovrtechnology.tutorial.spawn.TutorialJoinHandler;
import com.ovrtechnology.tutorial.spawn.TutorialSpawnManager;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypoint;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointAreaHandler;
import com.ovrtechnology.tutorial.waypoint.TutorialWaypointManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Subcommand to reset tutorial progress for players.
 * <p>
 * This resets EVERYTHING:
 * <ul>
 *   <li>Teleports player back to spawn point</li>
 *   <li>Stops any active cinematic</li>
 *   <li>Resets all chests to unconsumed</li>
 *   <li>Clears waypoint progress</li>
 *   <li>Resets intro sequence (title, particles, sound)</li>
 *   <li>Plays intro sequence again</li>
 * </ul>
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /tutorial reset} - Reset for executing player</li>
 *   <li>{@code /tutorial reset <player>} - Reset for specific player(s)</li>
 *   <li>{@code /tutorial reset all} - Reset for all online players</li>
 * </ul>
 */
public class ResetSubCommand implements TutorialSubCommand {

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Reset ALL tutorial progress (spawn, chests, cinematics, waypoints)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder
                // /tutorial reset - reset for self
                .executes(this::executeResetSelf)

                // /tutorial reset all - reset all online players
                .then(Commands.literal("all")
                        .executes(this::executeResetAll))

                // /tutorial reset <player> - reset specific player(s)
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(this::executeResetPlayers));
    }

    private int executeResetSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] This command must be executed by a player or specify a target"
            ));
            return 0;
        }

        // Re-enable map protection (in case it was bypassed)
        com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaHandler.disableBypass();

        // Reset all chests, animations, and regen areas
        ServerLevel level = source.getLevel();
        int chestsReset = TutorialChestManager.resetAllChests(level);
        int animationsReset = TutorialAnimationManager.resetAllAnimations(level);
        int blocksRestored = TutorialRegenAreaManager.restoreAllAreas(level);

        resetPlayer(player);

        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fYour tutorial progress has been completely reset!" +
                        (chestsReset > 0 ? " \u00a77(" + chestsReset + " chests)" : "") +
                        (animationsReset > 0 ? " \u00a77(" + animationsReset + " animations)" : "")),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeResetAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();

        if (players.isEmpty()) {
            source.sendFailure(Component.literal(
                    "\u00a7c[OVR Tutorial] No players online"
            ));
            return 0;
        }

        // Re-enable map protection (in case it was bypassed)
        com.ovrtechnology.tutorial.regenarea.TutorialRegenAreaHandler.disableBypass();

        // Reset all chests and animations once (not per player)
        ServerLevel level = source.getLevel();
        int chestsReset = TutorialChestManager.resetAllChests(level);
        TutorialAnimationManager.resetAllAnimations(level);

        for (ServerPlayer player : players) {
            resetPlayer(player);
        }

        int count = players.size();
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fReset tutorial for \u00a7d"
                        + count + "\u00a7f player" + (count == 1 ? "" : "s")
                        + " \u00a77(chests: " + chestsReset + ")"),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    private int executeResetPlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        Collection<ServerPlayer> players;
        try {
            players = EntityArgument.getPlayers(context, "players");
        } catch (Exception e) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] Invalid player selection"));
            return 0;
        }

        if (players.isEmpty()) {
            source.sendFailure(Component.literal("\u00a7c[OVR Tutorial] No matching players found"));
            return 0;
        }

        // Reset all chests and animations once (not per player)
        ServerLevel level = source.getLevel();
        int chestsReset = TutorialChestManager.resetAllChests(level);
        TutorialAnimationManager.resetAllAnimations(level);

        for (ServerPlayer player : players) {
            resetPlayer(player);
        }

        int count = players.size();
        source.sendSuccess(
                () -> Component.literal("\u00a7d[OVR Tutorial] \u00a7fReset tutorial for \u00a7d"
                        + count + "\u00a7f player" + (count == 1 ? "" : "s")
                        + " \u00a77(chests: " + chestsReset + ")"),
                true
        );

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Resets all tutorial progress for a player.
     * <p>
     * This performs a COMPLETE reset:
     * <ul>
     *   <li>Stops any active cinematic</li>
     *   <li>Teleports to spawn point</li>
     *   <li>Resets intro sequence state</li>
     *   <li>Resets waypoint progress</li>
     *   <li>Syncs chest state (so particles show again)</li>
     *   <li>Plays intro sequence</li>
     * </ul>
     *
     * @param player the player to reset
     */
    private void resetPlayer(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        // 0. Clear player inventory
        player.getInventory().clearContent();
        AromaAffect.LOGGER.debug("Reset: cleared inventory for player {}", player.getName().getString());

        // 0b. Restore full health and food
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);

        // 1. Stop any active cinematic (restores position and game mode)
        TutorialCinematicHandler.stopCinematic(player);

        // 1b. Force survival mode as safety net (in case cinematic stop failed
        // or player was left in spectator by a bug)
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

        // 2. Reset intro sequence state (allows title/particles to play again)
        TutorialJoinHandler.resetPlayer(player.getUUID());

        // 3. Reset waypoint progress (completed waypoints, active waypoint)
        TutorialWaypointAreaHandler.resetPlayer(player.getUUID());

        // 3b. Reset nose equip triggers (allows them to fire again)
        com.ovrtechnology.tutorial.noseequip.TutorialNoseEquipHandler.resetPlayer(player.getUUID());

        // 3c. Reset boss area triggers (allows boss to spawn again)
        com.ovrtechnology.tutorial.boss.TutorialBossAreaManager.get(level).resetPlayerTriggers(player.getUUID());
        com.ovrtechnology.tutorial.boss.TutorialBossAreaHandler.clearAllBosses();

        // 3d. Reset popup zone seen state (allows popups to show again)
        com.ovrtechnology.tutorial.popupzone.TutorialPopupZoneHandler.clearPlayer(player.getUUID());

        // 3e. Deactivate scent counter HUD
        com.ovrtechnology.network.TutorialScentCounterNetworking.sendDeactivate(player);

        // 3f. Reset finish screen trigger
        com.ovrtechnology.tutorial.finishscreen.TutorialFinishZoneHandler.resetPlayer(player.getUUID());

        // 3g. Reset SearchDiamond - end session, regenerate zone, clear hologram
        com.ovrtechnology.tutorial.searchdiamond.SearchDiamondZoneHandler.endSession(player, true);
        com.ovrtechnology.tutorial.searchdiamond.SearchDiamondZoneHandler.resetAll(level);
        com.ovrtechnology.network.SearchDiamondNetworking.sendClearHologram(player);

        // 4. Clear any active waypoint visuals on the client
        TutorialWaypointNetworking.sendClearToPlayer(player);

        // 5. Sync chest state to client (so particles show again for unconsumed chests)
        TutorialChestNetworking.syncAllChestsToPlayer(player);

        // 5b. Clear animation client state
        TutorialAnimationNetworking.sendAnimationReset(player);

        // 6. Sync custom dialogue texts to client
        TutorialDialogueContentNetworking.syncToPlayer(player, level);

        // 7. Teleport to spawn point FIRST (so chunks load)
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

            // 8. Play intro sequence (particles, sound, title)
            playIntroSequence(player, level);
        }

        // 9. Reset all Oliver and NoseSmith entities AFTER teleport (chunks now loaded)
        resetAllOlivers(level);

        // 10. Activate first waypoint if configured
        var firstWpOpt = TutorialSpawnManager.getFirstWaypointId(level);
        if (firstWpOpt.isPresent()) {
            String waypointId = firstWpOpt.get();
            var wpOpt = TutorialWaypointManager.getWaypoint(level, waypointId);
            if (wpOpt.isPresent() && wpOpt.get().isComplete()) {
                TutorialWaypoint wp = wpOpt.get();
                TutorialWaypointAreaHandler.setActiveWaypoint(player.getUUID(), waypointId);
                TutorialWaypointNetworking.sendWaypointToPlayer(player, waypointId, wp.getValidPositions());
                AromaAffect.LOGGER.info("Reset: activated first waypoint '{}' for player {}", waypointId, player.getName().getString());
            } else {
                AromaAffect.LOGGER.warn("Reset: first waypoint '{}' not found or incomplete", waypointId);
            }
        } else {
            AromaAffect.LOGGER.info("Reset: no first waypoint configured in spawn data");
        }
    }

    /**
     * Finds all Oliver entities in the level and resets them to home position.
     * Also resets all Nose Smith entities to initial quest state.
     * Force-loads the NoseSmith spawn chunk to ensure it's found even if far away.
     */
    private void resetAllOlivers(ServerLevel level) {
        // Reset all Olivers in loaded chunks
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof TutorialOliverEntity oliver) {
                oliver.resetToHome();
                AromaAffect.LOGGER.debug("Reset: Oliver teleported home to {}", oliver.getHomePos());
            }
        }

        // Reset NoseSmith - search near configured spawn position (force-loads chunk)
        var spawnOpt = com.ovrtechnology.tutorial.nosesmith.TutorialNoseSmithManager.getSpawnPos(level);
        if (spawnOpt.isPresent()) {
            net.minecraft.core.BlockPos spawnPos = spawnOpt.get();
            float spawnYaw = com.ovrtechnology.tutorial.nosesmith.TutorialNoseSmithManager.getSpawnYaw(level);

            // Force-load the chunk at spawn position
            level.getChunk(spawnPos);

            // Search in wide area around spawn (NoseSmith may have wandered far)
            net.minecraft.world.phys.AABB searchArea = new net.minecraft.world.phys.AABB(
                    spawnPos.getX() - 200, spawnPos.getY() - 100, spawnPos.getZ() - 200,
                    spawnPos.getX() + 200, spawnPos.getY() + 100, spawnPos.getZ() + 200
            );

            java.util.List<com.ovrtechnology.entity.nosesmith.NoseSmithEntity> noseSmiths =
                    level.getEntitiesOfClass(com.ovrtechnology.entity.nosesmith.NoseSmithEntity.class, searchArea);

            for (com.ovrtechnology.entity.nosesmith.NoseSmithEntity noseSmith : noseSmiths) {
                noseSmith.resetQuest();
                noseSmith.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                noseSmith.setYRot(spawnYaw);
                noseSmith.setYHeadRot(spawnYaw);
                noseSmith.setYBodyRot(spawnYaw);
                AromaAffect.LOGGER.debug("Reset: Nose Smith quest reset and teleported to {}", spawnPos);
            }

            if (noseSmiths.isEmpty()) {
                AromaAffect.LOGGER.warn("Reset: No NoseSmith found near spawn position {}", spawnPos);
            }
        } else {
            // No spawn configured, try all loaded entities
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof com.ovrtechnology.entity.nosesmith.NoseSmithEntity noseSmith) {
                    noseSmith.resetQuest();
                    AromaAffect.LOGGER.debug("Reset: Nose Smith quest reset (no spawn configured)");
                }
            }
        }
    }

    /**
     * Plays the intro sequence for a player.
     * Copied from TutorialJoinHandler to allow manual triggering.
     */
    private void playIntroSequence(ServerPlayer player, ServerLevel level) {
        // Title animation timings (in ticks): fade in, stay, fade out
        final int fadeIn = 20;   // 1 second
        final int stay = 60;     // 3 seconds
        final int fadeOut = 20;  // 1 second

        // Send title animation timing packet
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));

        // Title: "OVR TECHNOLOGY" in OVR purple (#A890F0), bold
        net.minecraft.network.chat.Component title = Component.literal("OVR TECHNOLOGY")
                .setStyle(net.minecraft.network.chat.Style.EMPTY
                        .withColor(net.minecraft.network.chat.TextColor.fromRgb(0xA890F0))
                        .withBold(true));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(title));

        // Subtitle: "Scent unlocked..." in white, italic
        net.minecraft.network.chat.Component subtitle = Component.literal("Scent unlocked...")
                .setStyle(net.minecraft.network.chat.Style.EMPTY
                        .withColor(net.minecraft.network.chat.TextColor.fromRgb(0xFFFFFF))
                        .withItalic(true));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(subtitle));

        // Play ambient sound
        player.playNotifySound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.AMBIENT, 1.0f, 0.8f);

        // Spawn particles around the player
        spawnIntroParticles(player, level);
    }

    /**
     * Spawns intro particles around a player.
     * Copied from TutorialJoinHandler to allow manual triggering.
     */
    private void spawnIntroParticles(ServerPlayer player, ServerLevel level) {
        net.minecraft.world.phys.Vec3 pos = player.position();
        net.minecraft.util.RandomSource random = level.getRandom();

        // Cherry blossom particles in a circle around the player
        final int particleCount = 30;
        for (int i = 0; i < particleCount; i++) {
            double angle = (2.0 * Math.PI * i) / particleCount;
            double radius = 2.0 + (random.nextDouble() * 1.5);
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;
            double y = pos.y + 0.5 + (random.nextDouble() * 2.0);

            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES,
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
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.x, pos.y + 1.0, pos.z,
                15,     // count
                0.5,    // xSpread
                0.8,    // ySpread
                0.5,    // zSpread
                0.05    // speed
        );
    }
}
