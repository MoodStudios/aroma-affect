package com.ovrtechnology.tutorial.portal;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.TutorialModule;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for portal teleportation.
 * <p>
 * Checks if players are inside portal source areas and teleports them
 * to the destination when they enter.
 */
public final class TutorialPortalHandler {

    /**
     * Cooldown to prevent rapid re-teleportation.
     * Key: Player UUID, Value: Tick when cooldown expires
     */
    private static final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();

    private static final int COOLDOWN_TICKS = 40; // 2 seconds cooldown
    private static final int CHECK_INTERVAL = 5;  // Check every 5 ticks

    private static boolean initialized = false;
    private static int tickCounter = 0;
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
            currentTick++;
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL) {
                return;
            }
            tickCounter = 0;

            // Check all players on all levels
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayerPortals(player);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial portal handler initialized");
    }

    /**
     * Checks if a player is inside any portal and teleports them.
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
        for (TutorialPortal portal : TutorialPortalManager.getCompletePortals(level)) {
            if (portal.isInsideSourceArea(playerPos)) {
                teleportPlayer(player, portal, level);
                return; // Only teleport to one portal per tick
            }
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

        // Set cooldown
        teleportCooldowns.put(player.getUUID(), currentTick + COOLDOWN_TICKS);

        // Spawn particles at source location
        spawnTeleportParticles(level, player.position().x, player.position().y, player.position().z);

        // Play teleport sound at source
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.8f, 1.2f
        );

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

        // Spawn particles at destination
        spawnTeleportParticles(level, dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5);

        // Play teleport sound at destination
        level.playSound(
                null,
                dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.8f, 1.0f
        );

        // Apply short blindness effect for smooth transition
        player.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                15,  // 0.75 seconds (15 ticks)
                0,   // Amplifier 0
                false, // Not ambient
                false, // No particles
                false  // No icon
        ));

        AromaAffect.LOGGER.debug("Player {} teleported via portal {}",
                player.getName().getString(), portal.getId());
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
     * Clears cooldown for a player.
     */
    public static void clearCooldown(UUID playerId) {
        teleportCooldowns.remove(playerId);
    }

    /**
     * Clears all cooldowns.
     */
    public static void clearAllCooldowns() {
        teleportCooldowns.clear();
    }
}
