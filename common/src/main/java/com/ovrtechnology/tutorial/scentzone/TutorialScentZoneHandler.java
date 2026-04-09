package com.ovrtechnology.tutorial.scentzone;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for scent zone area detection.
 * Triggers scents when players enter defined zones.
 */
public final class TutorialScentZoneHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10;

    /** Tracks which zones each player is currently inside. */
    private static final Map<UUID, Set<String>> playerInsideZones = new ConcurrentHashMap<>();

    /** Tracks cooldown end times per player per zone. */
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();

    /** Tracks one-shot zones that have already fired for each player. */
    private static final Map<UUID, Set<String>> playerOneShotFired = new ConcurrentHashMap<>();

    private TutorialScentZoneHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL) return;
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                if (!TutorialModule.isActive(level)) continue;
                checkPlayer(player, level);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial scent zone handler initialized");
    }

    private static void checkPlayer(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        BlockPos playerPos = player.blockPosition();
        long now = System.currentTimeMillis();

        Set<String> currentlyInside = playerInsideZones
                .computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());

        for (TutorialScentZone zone : TutorialScentZoneManager.getAllZones(level)) {
            if (!zone.isEnabled()) continue;

            String zoneId = zone.getId();
            boolean wasInside = currentlyInside.contains(zoneId);
            boolean isInside = zone.isInside(playerPos);

            if (isInside && !wasInside) {
                // Player just entered the zone
                currentlyInside.add(zoneId);

                if (canTrigger(playerId, zoneId, zone, now)) {
                    triggerZone(player, zone);
                    recordTrigger(playerId, zoneId, zone, now);
                }
            } else if (!isInside && wasInside) {
                // Player left the zone
                currentlyInside.remove(zoneId);
            }
        }
    }

    private static boolean canTrigger(UUID playerId, String zoneId, TutorialScentZone zone, long now) {
        // Check one-shot
        if (zone.isOneShot()) {
            Set<String> fired = playerOneShotFired.get(playerId);
            if (fired != null && fired.contains(zoneId)) {
                return false;
            }
        }

        // Check cooldown
        Map<String, Long> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns != null) {
            Long cooldownEnd = cooldowns.get(zoneId);
            if (cooldownEnd != null && now < cooldownEnd) {
                return false;
            }
        }

        return true;
    }

    private static void recordTrigger(UUID playerId, String zoneId, TutorialScentZone zone, long now) {
        if (zone.isOneShot()) {
            playerOneShotFired
                    .computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                    .add(zoneId);
        }

        if (zone.getCooldownSeconds() > 0) {
            playerCooldowns
                    .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                    .put(zoneId, now + (zone.getCooldownSeconds() * 1000L));
        }
    }

    private static void triggerZone(ServerPlayer player, TutorialScentZone zone) {
        // Force 100% intensity for all zones
        TutorialScentZoneNetworking.sendScentTrigger(
                player, zone.getScentName(), 1.0, zone.getId()
        );
        AromaAffect.LOGGER.info("Scent zone '{}' triggered for player {} (scent: {}, intensity: {})",
                zone.getId(), player.getName().getString(), zone.getScentName(), zone.getIntensity());
    }

    /**
     * Resets all tracking for a player (on tutorial restart).
     */
    public static void resetPlayer(UUID playerId) {
        playerInsideZones.remove(playerId);
        playerCooldowns.remove(playerId);
        playerOneShotFired.remove(playerId);
    }

    /**
     * Resets all tracking for all players.
     */
    public static void resetAll() {
        playerInsideZones.clear();
        playerCooldowns.clear();
        playerOneShotFired.clear();
    }
}
