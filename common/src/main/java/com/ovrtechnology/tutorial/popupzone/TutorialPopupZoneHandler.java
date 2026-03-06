package com.ovrtechnology.tutorial.popupzone;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.compat.ReplayCompat;
import com.ovrtechnology.network.TutorialPopupNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for popup HUD zones.
 * <p>
 * Checks player positions against popup zones every 10 ticks.
 * Sends popup text when entering, clears when leaving.
 */
public final class TutorialPopupZoneHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10;

    // Track which zone each player is currently in
    private static final Map<UUID, String> playerCurrentZone = new ConcurrentHashMap<>();

    // Track which zones each player has already seen (won't show again until reset)
    private static final Map<UUID, Set<String>> playerSeenZones = new ConcurrentHashMap<>();

    private TutorialPopupZoneHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            if (ReplayCompat.isReplayServer(server)) return;
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL) return;
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                if (!TutorialModule.isActive(level)) continue;
                checkPlayerZones(player, level);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial popup zone handler initialized");
    }

    private static void checkPlayerZones(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        BlockPos playerPos = player.blockPosition();

        Set<String> seen = playerSeenZones.computeIfAbsent(playerId, k -> new HashSet<>());

        // Find which unseen zone the player is in
        String currentZoneId = null;
        TutorialPopupZone currentZone = null;
        for (TutorialPopupZone zone : TutorialPopupZoneManager.getCompleteZones(level)) {
            if (zone.isInsideArea(playerPos) && !seen.contains(zone.getId())) {
                currentZoneId = zone.getId();
                currentZone = zone;
                break;
            }
        }

        String previousZoneId = playerCurrentZone.get(playerId);

        if (currentZoneId != null) {
            if (!currentZoneId.equals(previousZoneId)) {
                // Entered a new unseen zone (or switched zones)
                playerCurrentZone.put(playerId, currentZoneId);
                TutorialPopupNetworking.sendPopup(player, currentZone.getText());
            }
        } else {
            if (previousZoneId != null) {
                // Left the zone — mark as seen
                seen.add(previousZoneId);
                playerCurrentZone.remove(playerId);
                TutorialPopupNetworking.sendClearPopup(player);
            }
        }
    }

    public static void clearPlayer(UUID playerId) {
        playerCurrentZone.remove(playerId);
        playerSeenZones.remove(playerId);
    }

    public static void clearAll() {
        playerCurrentZone.clear();
        playerSeenZones.clear();
    }
}
