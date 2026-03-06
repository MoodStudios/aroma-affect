package com.ovrtechnology.tutorial.portal;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import com.ovrtechnology.network.TutorialPortalOverlayNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.oliver.TutorialOliverEntity;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for portal teleportation.
 * <p>
 * Checks if players are inside portal source areas and teleports them
 * to the destination after a buildup period with visual overlay.
 */
public final class TutorialPortalHandler {

    /**
     * Cooldown to prevent rapid re-teleportation.
     * Key: Player UUID, Value: Tick when cooldown expires
     */
    private static final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();

    /**
     * Tracks how long each player has been inside a portal.
     * Key: Player UUID, Value: Ticks spent in portal
     */
    private static final Map<UUID, Integer> portalProgress = new ConcurrentHashMap<>();

    /**
     * Tracks which portal each player is currently in.
     * Key: Player UUID, Value: Portal ID
     */
    private static final Map<UUID, String> playerInPortal = new ConcurrentHashMap<>();

    private static final int COOLDOWN_TICKS = 20;       // 1 second cooldown after teleport
    private static final int PORTAL_TIME_TICKS = 30;    // 1.5 seconds to teleport
    private static final int CHECK_INTERVAL = 2;        // Check every 2 ticks for smooth overlay
    private static final int OVERLAY_UPDATE_INTERVAL = 4; // Update overlay every 4 ticks

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static int overlayTickCounter = 0;
    private static long currentTick = 0;

    // Purple particle for portal effect
    private static final DustParticleOptions PURPLE_PARTICLE = new DustParticleOptions(
            0xFFA890F0,  // OVR Purple
            1.2f
    );

    private TutorialPortalHandler() {
    }

    /**
     * Initializes the portal handler.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            if (ReplayCompat.isReplayServer(server)) return;
            currentTick++;
            tickCounter++;
            overlayTickCounter++;

            if (tickCounter < CHECK_INTERVAL) {
                return;
            }
            tickCounter = 0;

            // Check all players on all levels
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayerPortals(player);
            }

            // Send overlay updates less frequently
            if (overlayTickCounter >= OVERLAY_UPDATE_INTERVAL) {
                overlayTickCounter = 0;
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    sendOverlayUpdate(player);
                }
            }
        });

        AromaAffect.LOGGER.debug("Tutorial portal handler initialized");
    }

    /**
     * Checks if a player is inside any portal and handles buildup/teleportation.
     */
    private static void checkPlayerPortals(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // Check if tutorial mode is active
        if (!TutorialModule.isActive(level)) {
            return;
        }

        UUID playerId = player.getUUID();

        // Check cooldown
        Long cooldownEnd = teleportCooldowns.get(playerId);
        if (cooldownEnd != null && currentTick < cooldownEnd) {
            return;
        }

        BlockPos playerPos = player.blockPosition();

        // Check all complete portals
        TutorialPortal currentPortal = null;
        for (TutorialPortal portal : TutorialPortalManager.getCompletePortals(level)) {
            if (portal.isInsideSourceArea(playerPos)) {
                currentPortal = portal;
                break;
            }
        }

        String previousPortalId = playerInPortal.get(playerId);

        if (currentPortal != null) {
            String currentPortalId = currentPortal.getId();

            // Check if player switched portals
            if (previousPortalId != null && !previousPortalId.equals(currentPortalId)) {
                // Reset progress for new portal
                portalProgress.put(playerId, 0);
            }

            // Track that player is in this portal
            playerInPortal.put(playerId, currentPortalId);

            // Increment portal progress
            int progress = portalProgress.getOrDefault(playerId, 0) + CHECK_INTERVAL;
            portalProgress.put(playerId, progress);

            // Play portal ambient sound periodically
            if (progress % 20 == 0) {
                level.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PORTAL_AMBIENT,
                        SoundSource.BLOCKS,
                        0.5f, 1.0f
                );
            }

            // Use per-portal delay if configured, otherwise default
            int requiredTicks = currentPortal.hasCustomDelay() ? currentPortal.getDelayTicks() : PORTAL_TIME_TICKS;

            // Check if ready to teleport
            if (progress >= requiredTicks) {
                teleportPlayer(player, currentPortal, level);
                // Reset progress after teleport
                portalProgress.remove(playerId);
                playerInPortal.remove(playerId);
            }
        } else {
            // Player left the portal
            if (previousPortalId != null) {
                portalProgress.remove(playerId);
                playerInPortal.remove(playerId);
                // Clear overlay on client
                TutorialPortalOverlayNetworking.sendClearOverlay(player);
            }
        }
    }

    /**
     * Sends overlay progress update to a player.
     */
    private static void sendOverlayUpdate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        UUID playerId = player.getUUID();
        Integer progress = portalProgress.get(playerId);
        String portalId = playerInPortal.get(playerId);

        if (progress != null && progress > 0 && portalId != null) {
            // Use per-portal delay for accurate overlay progress
            int requiredTicks = PORTAL_TIME_TICKS;
            for (TutorialPortal portal : TutorialPortalManager.getCompletePortals(level)) {
                if (portal.getId().equals(portalId) && portal.hasCustomDelay()) {
                    requiredTicks = portal.getDelayTicks();
                    break;
                }
            }
            float overlayProgress = Math.min(1.0f, (float) progress / requiredTicks);
            TutorialPortalOverlayNetworking.sendOverlayProgress(player, overlayProgress);
        }
    }

    /**
     * Teleports a player through a portal.
     */
    private static void teleportPlayer(ServerPlayer player, TutorialPortal portal, ServerLevel level) {
        BlockPos dest = portal.getDestination();
        if (dest == null) {
            return;
        }

        // Find Oliver near the player BEFORE teleporting
        TutorialOliverEntity oliver = findNearestOliver(level, player.getX(), player.getY(), player.getZ());

        // Flash overlay to full before clearing (dimensional flash)
        TutorialPortalOverlayNetworking.sendOverlayProgress(player, 1.0f);

        // Set cooldown
        teleportCooldowns.put(player.getUUID(), currentTick + COOLDOWN_TICKS);

        // === SOURCE DIMENSIONAL EFFECT ===
        double srcX = player.position().x;
        double srcY = player.position().y;
        double srcZ = player.position().z;

        // Purple spiral
        spawnTeleportParticles(level, srcX, srcY, srcZ);

        // Dimensional vortex: portal particles swirling inward
        level.sendParticles(ParticleTypes.PORTAL, srcX, srcY + 1, srcZ,
                60, 1.5, 1.5, 1.5, 1.0);

        // Reverse portal burst (pulling in)
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, srcX, srcY + 1, srcZ,
                40, 0.5, 1.0, 0.5, 0.3);

        // Witch magic sparkles
        level.sendParticles(ParticleTypes.WITCH, srcX, srcY + 0.5, srcZ,
                25, 0.8, 1.0, 0.8, 0.1);

        // Dimensional rift sound (end portal + enderman teleport)
        level.playSound(null, srcX, srcY, srcZ,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6f, 1.5f);
        level.playSound(null, srcX, srcY, srcZ,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.7f);

        // Teleport player
        player.teleportTo(
                level,
                dest.getX() + 0.5,
                dest.getY(),
                dest.getZ() + 0.5,
                Set.of(),
                portal.getDestYaw(),
                portal.getDestPitch(),
                false
        );

        // Teleport Oliver along with the player (offset slightly so they don't overlap)
        if (oliver != null) {
            // Spawn particles at Oliver's source location
            spawnTeleportParticles(level, oliver.getX(), oliver.getY(), oliver.getZ());

            // Teleport Oliver near the destination (2 blocks offset)
            double oliverDestX = dest.getX() + 2.5;
            double oliverDestY = dest.getY();
            double oliverDestZ = dest.getZ() + 0.5;
            oliver.teleportTo(oliverDestX, oliverDestY, oliverDestZ);

            // Make Oliver face the player
            oliver.setYRot(portal.getDestYaw() + 180);
            oliver.setYHeadRot(portal.getDestYaw() + 180);

            // Spawn particles at Oliver's destination
            spawnTeleportParticles(level, oliverDestX, oliverDestY + 1, oliverDestZ);

            AromaAffect.LOGGER.debug("Oliver teleported with player via portal {}", portal.getId());
        }

        // === DESTINATION DIMENSIONAL EFFECT ===
        double dstX = dest.getX() + 0.5;
        double dstY = dest.getY();
        double dstZ = dest.getZ() + 0.5;

        // Purple spiral at destination
        spawnTeleportParticles(level, dstX, dstY + 1, dstZ);

        // Materialization burst: end rod + enchant expanding outward
        level.sendParticles(ParticleTypes.END_ROD, dstX, dstY + 1.5, dstZ,
                35, 0.2, 0.5, 0.2, 0.15);
        level.sendParticles(ParticleTypes.ENCHANT, dstX, dstY + 0.5, dstZ,
                40, 0.8, 0.3, 0.8, 0.5);

        // Reverse portal (expanding outward from arrival point)
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, dstX, dstY + 1, dstZ,
                50, 0.3, 0.8, 0.3, 0.5);

        // Smoke burst for dramatic arrival
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, dstX, dstY + 0.5, dstZ,
                10, 0.5, 0.1, 0.5, 0.02);

        // Arrival sounds
        level.playSound(null, dstX, dstY, dstZ,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, dstX, dstY, dstZ,
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.5f, 1.4f);

        // Clear overlay after a brief flash
        TutorialPortalOverlayNetworking.sendClearOverlay(player);

        // Apply darkness + blindness for dramatic dimensional transition
        player.addEffect(new MobEffectInstance(
                MobEffects.DARKNESS,
                25,  // 1.25 seconds
                0,
                false, false, false
        ));
        player.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                15,  // 0.75 seconds
                0,
                false, false, false
        ));

        AromaAffect.LOGGER.debug("Player {} teleported via portal {} (dimensional effect)",
                player.getName().getString(), portal.getId());
    }

    /**
     * Finds the nearest Oliver entity within search range of a position.
     */
    private static TutorialOliverEntity findNearestOliver(ServerLevel level, double x, double y, double z) {
        AABB searchArea = new AABB(
                x - 50, y - 25, z - 50,
                x + 50, y + 25, z + 50
        );

        List<TutorialOliverEntity> olivers = level.getEntitiesOfClass(
                TutorialOliverEntity.class, searchArea
        );

        if (olivers.isEmpty()) {
            return null;
        }

        TutorialOliverEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (TutorialOliverEntity oliver : olivers) {
            double dist = oliver.distanceToSqr(x, y, z);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = oliver;
            }
        }

        return nearest;
    }

    /**
     * Spawns purple spiral particles for teleport effect.
     */
    private static void spawnTeleportParticles(ServerLevel level, double x, double y, double z) {
        // Double helix spiral particles
        for (int i = 0; i < 40; i++) {
            double angle = (Math.PI * 4 * i) / 40; // 2 full rotations
            double radius = 0.6 + (i * 0.02);
            double py = y + (i * 0.06);

            // First helix
            double px1 = x + Math.cos(angle) * radius;
            double pz1 = z + Math.sin(angle) * radius;
            level.sendParticles(
                    PURPLE_PARTICLE,
                    px1, py, pz1,
                    2,
                    0.05, 0.05, 0.05,
                    0.01
            );

            // Second helix (offset by 180 degrees)
            double px2 = x + Math.cos(angle + Math.PI) * radius;
            double pz2 = z + Math.sin(angle + Math.PI) * radius;
            level.sendParticles(
                    PURPLE_PARTICLE,
                    px2, py, pz2,
                    2,
                    0.05, 0.05, 0.05,
                    0.01
            );
        }

        // Central burst of particles
        level.sendParticles(
                PURPLE_PARTICLE,
                x, y + 1.0, z,
                30,
                0.3, 0.5, 0.3,
                0.1
        );

        // Sparkle effect
        level.sendParticles(
                ParticleTypes.END_ROD,
                x, y + 1.0, z,
                15,
                0.4, 0.6, 0.4,
                0.05
        );
    }

    /**
     * Clears cooldown and portal progress for a player.
     */
    public static void clearCooldown(UUID playerId) {
        teleportCooldowns.remove(playerId);
        portalProgress.remove(playerId);
        playerInPortal.remove(playerId);
    }

    /**
     * Clears all cooldowns and portal progress.
     */
    public static void clearAllCooldowns() {
        teleportCooldowns.clear();
        portalProgress.clear();
        playerInPortal.clear();
    }

    /**
     * Called when a player leaves the server.
     */
    public static void onPlayerLeave(UUID playerId) {
        teleportCooldowns.remove(playerId);
        portalProgress.remove(playerId);
        playerInPortal.remove(playerId);
    }
}
