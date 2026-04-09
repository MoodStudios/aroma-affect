package com.ovrtechnology.tutorial.scentzone;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Triggers Marine scent when the player touches water.
 * Resets when the player leaves water, so re-entering triggers again.
 */
public final class TutorialWaterHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10;

    private static final Map<UUID, Boolean> wasInWater = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> hasFired = new ConcurrentHashMap<>();

    private TutorialWaterHandler() {}

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
                checkPlayer(player);
            }
        });

        AromaAffect.LOGGER.debug("Tutorial water handler initialized");
    }

    private static void checkPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        boolean inWater = player.isInWater();
        boolean wasIn = wasInWater.getOrDefault(playerId, false);

        wasInWater.put(playerId, inWater);

        if (inWater && !wasIn) {
            hasFired.put(playerId, false);
        }

        if (!inWater && wasIn) {
            hasFired.put(playerId, false);
        }

        if (inWater && !hasFired.getOrDefault(playerId, false)) {
            hasFired.put(playerId, true);
            TutorialScentZoneNetworking.sendScentTrigger(player, "Marine", 1.0, "water_touch");
            AromaAffect.LOGGER.info("Player {} touched water, triggering Marine scent", player.getName().getString());
        }
    }

    public static void resetPlayer(UUID playerId) {
        wasInWater.remove(playerId);
        hasFired.remove(playerId);
    }

    public static void resetAll() {
        wasInWater.clear();
        hasFired.clear();
    }
}
