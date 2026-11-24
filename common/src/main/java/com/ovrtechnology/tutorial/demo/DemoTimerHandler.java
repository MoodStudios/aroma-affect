package com.ovrtechnology.tutorial.demo;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import com.ovrtechnology.network.TutorialFinishNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional session countdown, shown on the HUD under the scent counter. Its length comes from
 * {@link DemoConfig}; with no limit set there is no countdown and nothing cuts a session short.
 * Starts when the player enters Play Demo or Walkaround.
 * When time runs out, shows the end screen, where the visitor can keep playing with a new
 * countdown or leave it to start the next game.
 */
public final class DemoTimerHandler {

    private static boolean initialized = false;

    private static final Map<UUID, TimerState> activeTimers = new ConcurrentHashMap<>();

    private DemoTimerHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                if (!TutorialModule.isActive(level)) continue;

                TimerState state = activeTimers.get(player.getUUID());
                if (state == null) continue;

                state.ticksRemaining--;

                // The HUD shows whole seconds, so once a second is enough
                if (state.ticksRemaining % 20 == 0 && state.ticksRemaining > 0) {
                    TutorialFinishNetworking.sendTimer(player, state.ticksRemaining / 20);
                }

                if (state.ticksRemaining <= 0) {
                    activeTimers.remove(player.getUUID());
                    onTimeUp(player);
                }
            }
        });

        AromaAffect.LOGGER.debug("Demo timer handler initialized");
    }

    /**
     * Starts the session timer for a player.
     */
    public static void startTimer(ServerPlayer player) {
        // Remove any existing timer
        stopTimer(player);

        int totalSeconds = DemoConfig.get().sessionSeconds();
        if (totalSeconds <= 0) {
            AromaAffect.LOGGER.info("Demo session started for player {} with no time limit", player.getName().getString());
            return;
        }
        activeTimers.put(player.getUUID(), new TimerState(totalSeconds * 20));
        TutorialFinishNetworking.sendTimer(player, totalSeconds);
        AromaAffect.LOGGER.info("Demo timer started for player {} ({} seconds)", player.getName().getString(), totalSeconds);
    }

    /**
     * Stops the timer for a player. Sends nothing: it also runs while the player disconnects,
     * and the client forgets the countdown when it leaves the world.
     */
    public static void stopTimer(ServerPlayer player) {
        if (player == null) return;
        activeTimers.remove(player.getUUID());
    }

    /**
     * "Continue playing" on the end screen: the same visitor keeps going with a full new countdown.
     */
    public static void continueSession(ServerPlayer player) {
        startTimer(player);
        AromaAffect.LOGGER.info("Player {} kept playing after the end screen", player.getName().getString());
    }

    /**
     * Forgets every countdown. For when the server they ran on is gone.
     */
    public static void clearAll() {
        activeTimers.clear();
    }

    /**
     * Called when the timer reaches zero.
     */
    private static void onTimeUp(ServerPlayer player) {
        TutorialFinishNetworking.sendTimer(player, 0);
        // End screen for a session that is over: keep playing or hand the station to the next visitor
        TutorialFinishNetworking.sendOpenFinish(player, true);
        AromaAffect.LOGGER.info("Demo timer expired for player {} — showing finish screen", player.getName().getString());
    }

    /**
     * Ends the session now, as if the time had run out. Used when the player chooses to leave early.
     */
    public static void endSession(ServerPlayer player) {
        stopTimer(player);
        TutorialFinishNetworking.sendTimer(player, 0);
        TutorialFinishNetworking.sendOpenFinish(player, true);
        AromaAffect.LOGGER.info("Player {} ended the demo early", player.getName().getString());
    }

    /**
     * Checks if a player has an active timer.
     */
    public static boolean hasTimer(UUID playerId) {
        return activeTimers.containsKey(playerId);
    }

    /**
     * Skips the timer to the last 10 seconds for testing.
     */
    public static void skipTimer(ServerPlayer player) {
        TimerState state = activeTimers.get(player.getUUID());
        if (state != null) {
            state.ticksRemaining = 200; // 10 seconds left
            TutorialFinishNetworking.sendTimer(player, 10);
            AromaAffect.LOGGER.info("Timer skipped for player {}", player.getName().getString());
        }
    }

    private static class TimerState {
        int ticksRemaining;

        TimerState(int totalTicks) {
            this.ticksRemaining = totalTicks;
        }
    }
}
