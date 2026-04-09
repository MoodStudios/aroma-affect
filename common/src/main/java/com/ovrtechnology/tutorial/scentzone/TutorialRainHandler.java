package com.ovrtechnology.tutorial.scentzone;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.TutorialModule;
import com.ovrtechnology.tutorial.cinematic.TutorialCinematicHandler;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Triggers Petrichor scent when the player is exposed to rain.
 * Resets when the player is no longer exposed, so re-exposure triggers again.
 * Does NOT fire during cinematics.
 */
public final class TutorialRainHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // every 1 second

    /** Tracks whether each player was exposed to rain last check. */
    private static final Map<UUID, Boolean> wasExposed = new ConcurrentHashMap<>();

    /** Tracks whether the scent already fired for the current exposure. */
    private static final Map<UUID, Boolean> hasFired = new ConcurrentHashMap<>();

    private TutorialRainHandler() {}

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

        AromaAffect.LOGGER.debug("Tutorial rain handler initialized");
    }

    private static void checkPlayer(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();

        // Don't trigger during cinematics
        if (TutorialCinematicHandler.isInCinematic(player)) {
            return;
        }

        boolean exposed = level.isRaining() && level.canSeeSky(player.blockPosition());

        boolean previouslyExposed = wasExposed.getOrDefault(playerId, false);
        wasExposed.put(playerId, exposed);

        if (exposed && !previouslyExposed) {
            // Just entered rain — reset fired flag
            hasFired.put(playerId, false);
        }

        if (!exposed && previouslyExposed) {
            // Left the rain — reset so next exposure triggers again
            hasFired.put(playerId, false);
        }

        if (exposed && !hasFired.getOrDefault(playerId, false)) {
            // Exposed and hasn't fired yet
            hasFired.put(playerId, true);
            TutorialScentZoneNetworking.sendScentTrigger(player, "Petrichor", 1.0, "rain_exposure");
            AromaAffect.LOGGER.info("Player {} exposed to rain, triggering Petrichor scent", player.getName().getString());
        }
    }

    public static void resetPlayer(UUID playerId) {
        wasExposed.remove(playerId);
        hasFired.remove(playerId);
    }

    public static void resetAll() {
        wasExposed.clear();
        hasFired.clear();
    }
}
