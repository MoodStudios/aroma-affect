package com.ovrtechnology.tutorial.popupzone;

import com.ovrtechnology.AromaAffect;
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
 * Some popups are "sticky" — they persist until a specific action is completed,
 * even after the player leaves the trigger zone.
 */
public final class TutorialPopupZoneHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10;

    /** Popup zone IDs that stick until dismissed by action completion. */
    private static final Set<String> STICKY_ZONES = Set.of(
            "find_nosesmith", "riron", "cowweb"
    );

    // Track which zone each player is currently in
    private static final Map<UUID, String> playerCurrentZone = new ConcurrentHashMap<>();

    // Track which zones each player has already seen (won't show again until reset)
    private static final Map<UUID, Set<String>> playerSeenZones = new ConcurrentHashMap<>();

    // Track active sticky popup per player (shown even outside zone)
    private static final Map<UUID, String> playerStickyPopup = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerStickyText = new ConcurrentHashMap<>();

    private TutorialPopupZoneHandler() {
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
                // Entered a new unseen zone
                playerCurrentZone.put(playerId, currentZoneId);
                TutorialPopupNetworking.sendPopup(player, currentZone.getText());

                // If it's a sticky zone, remember it
                if (STICKY_ZONES.contains(currentZoneId)) {
                    playerStickyPopup.put(playerId, currentZoneId);
                    playerStickyText.put(playerId, currentZone.getText());
                }
            }
        } else {
            if (previousZoneId != null) {
                // Left the zone
                playerCurrentZone.remove(playerId);

                if (STICKY_ZONES.contains(previousZoneId)) {
                    // Sticky — keep showing it, don't clear
                    // The popup stays visible until dismissStickyPopup is called
                } else {
                    // Non-sticky — mark as seen and clear
                    seen.add(previousZoneId);
                    TutorialPopupNetworking.sendClearPopup(player);
                }
            }
        }
    }

    /**
     * Dismisses a sticky popup for a player.
     * Called when the player completes the associated action.
     */
    public static void dismissStickyPopup(ServerPlayer player, String zoneId) {
        UUID playerId = player.getUUID();
        String currentSticky = playerStickyPopup.get(playerId);

        if (zoneId.equals(currentSticky)) {
            playerStickyPopup.remove(playerId);
            playerStickyText.remove(playerId);
            playerSeenZones.computeIfAbsent(playerId, k -> new HashSet<>()).add(zoneId);
            TutorialPopupNetworking.sendClearPopup(player);
            AromaAffect.LOGGER.info("Dismissed sticky popup '{}' for player {}", zoneId, player.getName().getString());
        }
    }

    /**
     * Dismisses ALL sticky popups for a player.
     * Called when the player progresses to the next stage (waypoint/trade completed).
     */
    public static void dismissAllSticky(ServerPlayer player) {
        UUID playerId = player.getUUID();
        String currentSticky = playerStickyPopup.get(playerId);

        if (currentSticky != null) {
            playerStickyPopup.remove(playerId);
            playerStickyText.remove(playerId);
            playerSeenZones.computeIfAbsent(playerId, k -> new HashSet<>()).add(currentSticky);
            TutorialPopupNetworking.sendClearPopup(player);
            AromaAffect.LOGGER.info("Dismissed all sticky popups for player {} (stage progressed)", player.getName().getString());
        }
    }

    public static void clearPlayer(UUID playerId) {
        playerCurrentZone.remove(playerId);
        playerSeenZones.remove(playerId);
        playerStickyPopup.remove(playerId);
        playerStickyText.remove(playerId);
    }

    public static void clearAll() {
        playerCurrentZone.clear();
        playerSeenZones.clear();
        playerStickyPopup.clear();
        playerStickyText.clear();
    }
}
