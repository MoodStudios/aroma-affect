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
 * 15-minute countdown timer displayed as a boss bar.
 * Starts when the player enters Play Demo or Walkaround.
 * When time runs out, shows the finish screen (QR codes) without "Continue" button.
 */
public final class DemoTimerHandler {

    private static boolean initialized = false;
    private static final int TOTAL_SECONDS = 15 * 60; // 15 minutes

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
                    state.bossBar.setProgress(Math.max(0f, (float) state.ticksRemaining / (TOTAL_SECONDS * 20)));
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
     * Starts the 15-minute timer for a player.
     */
    public static void startTimer(ServerPlayer player) {
        // Remove any existing timer
        stopTimer(player);

        ServerBossEvent bossBar = new ServerBossEvent(
                Component.literal("§d§l15:00"),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0f);

        activeTimers.put(player.getUUID(), new TimerState(bossBar, TOTAL_SECONDS * 20));
        AromaAffect.LOGGER.info("Demo timer started for player {} (15 minutes)", player.getName().getString());
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
        int ticksRemaining;

        TimerState(ServerBossEvent bossBar, int ticksRemaining) {
            this.bossBar = bossBar;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
