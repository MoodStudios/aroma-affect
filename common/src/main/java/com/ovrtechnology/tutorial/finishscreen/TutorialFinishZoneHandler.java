package com.ovrtechnology.tutorial.finishscreen;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialFinishNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for finish screen trigger zones.
 */
public final class TutorialFinishZoneHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10;

    private static final Set<UUID> triggeredPlayers = ConcurrentHashMap.newKeySet();

    private TutorialFinishZoneHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Tick handler: check player positions
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

        AromaAffect.LOGGER.debug("Tutorial finish zone handler initialized");
    }

    private static void checkPlayer(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        if (triggeredPlayers.contains(playerId)) return;

        BlockPos playerPos = player.blockPosition();
        for (TutorialFinishZone zone : TutorialFinishZoneManager.getCompleteZones(level)) {
            if (zone.isInsideArea(playerPos)) {
                triggeredPlayers.add(playerId);
                TutorialFinishNetworking.sendOpenFinish(player);
                AromaAffect.LOGGER.info("Player {} entered finish zone '{}', showing finish screen",
                        player.getName().getString(), zone.getId());
                return;
            }
        }
    }

    public static void resetPlayer(UUID playerId) {
        triggeredPlayers.remove(playerId);
    }

    public static void resetAll() {
        triggeredPlayers.clear();
    }
}
