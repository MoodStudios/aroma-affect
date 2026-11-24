package com.ovrtechnology.tutorial.demo;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.TutorialModule;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional session countdown displayed as a boss bar. Its length comes from {@link DemoConfig};
 * with no limit set there is no countdown and nothing cuts a session short.
 * Starts when the player enters Play Demo or Walkaround.
 * When time runs out, shows the end screen (QR codes), which then starts the next game.
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

                // Update boss bar every 20 ticks (1 second)
                if (state.ticksRemaining % 20 == 0 || state.ticksRemaining <= 0) {
                    int secondsLeft = Math.max(0, state.ticksRemaining / 20);
                    int minutes = secondsLeft / 60;
                    int seconds = secondsLeft % 60;
                    String timeStr = String.format("%02d:%02d", minutes, seconds);

                    // Color changes as time runs low
                    BossEvent.BossBarColor color;
                    if (secondsLeft <= 60) {
                        color = BossEvent.BossBarColor.RED;
                    } else if (secondsLeft <= 180) {
                        color = BossEvent.BossBarColor.YELLOW;
                    } else {
                        color = BossEvent.BossBarColor.PURPLE;
                    }

                    state.bossBar.setName(Component.literal("§d§l" + timeStr));
                    state.bossBar.setProgress(Math.max(0f, (float) state.ticksRemaining / state.totalTicks));
                    state.bossBar.setColor(color);
                }

                // Time's up
                if (state.ticksRemaining <= 0) {
                    onTimeUp(player);
                    activeTimers.remove(player.getUUID());
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
        ServerBossEvent bossBar = new ServerBossEvent(
                Component.literal(String.format("§d§l%02d:%02d", totalSeconds / 60, totalSeconds % 60)),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0f);

        activeTimers.put(player.getUUID(), new TimerState(bossBar, totalSeconds * 20));
        AromaAffect.LOGGER.info("Demo timer started for player {} ({} seconds)", player.getName().getString(), totalSeconds);
    }

    /**
     * Stops the timer for a player.
     */
    public static void stopTimer(ServerPlayer player) {
        TimerState state = activeTimers.remove(player.getUUID());
        if (state != null) {
            state.bossBar.removeAllPlayers();
        }
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
        TimerState state = activeTimers.get(player.getUUID());
        if (state != null) {
            state.bossBar.removeAllPlayers();
        }

        // Show finish screen (QR codes) — time expired version (no continue button)
        com.ovrtechnology.network.TutorialFinishNetworking.sendOpenFinish(player, true);
        AromaAffect.LOGGER.info("Demo timer expired for player {} — showing finish screen", player.getName().getString());
    }

    /**
     * Ends the session now, as if the time had run out. Used when the player chooses to leave early.
     */
    public static void endSession(ServerPlayer player) {
        stopTimer(player);
        com.ovrtechnology.network.TutorialFinishNetworking.sendOpenFinish(player, true);
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
            AromaAffect.LOGGER.info("Timer skipped for player {}", player.getName().getString());
        }
    }

    private static class TimerState {
        final ServerBossEvent bossBar;
        final int totalTicks;
        int ticksRemaining;

        TimerState(ServerBossEvent bossBar, int totalTicks) {
            this.bossBar = bossBar;
            this.totalTicks = totalTicks;
            this.ticksRemaining = totalTicks;
        }
    }
}
