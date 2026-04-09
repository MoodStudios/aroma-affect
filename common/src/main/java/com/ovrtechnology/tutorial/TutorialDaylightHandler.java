package com.ovrtechnology.tutorial;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;

/**
 * Keeps the tutorial world at daytime with clear weather.
 * Rain can be temporarily allowed via {@link #allowRainUntil(long)}.
 */
public final class TutorialDaylightHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 100; // Check every 5 seconds

    private static final long TIME_NOON = 6000L;
    private static final long TIME_SUNSET = 12000L;

    /** Timestamp (System.currentTimeMillis) until which rain is allowed. */
    private static long rainAllowedUntil = 0;

    private TutorialDaylightHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        TickEvent.SERVER_POST.register(server -> {
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL) return;
            tickCounter = 0;

            for (ServerLevel level : server.getAllLevels()) {
                if (!TutorialModule.isActive(level)) continue;

                // Keep it daytime
                long dayTime = level.getDayTime() % 24000L;
                if (dayTime >= TIME_SUNSET) {
                    level.setDayTime(TIME_NOON);
                }

                // Clear weather — unless rain is temporarily allowed
                if ((level.isRaining() || level.isThundering()) && !isRainAllowed()) {
                    level.setWeatherParameters(6000, 0, false, false);
                }
            }
        });

        AromaAffect.LOGGER.debug("Tutorial daylight handler initialized");
    }

    /**
     * Allows rain for the specified duration in milliseconds.
     * The daylight handler will not clear weather until this time passes.
     */
    public static void allowRainFor(long durationMs) {
        rainAllowedUntil = System.currentTimeMillis() + durationMs;
        AromaAffect.LOGGER.info("Rain allowed for {} ms", durationMs);
    }

    /**
     * Checks if rain is currently allowed.
     */
    public static boolean isRainAllowed() {
        return System.currentTimeMillis() < rainAllowedUntil;
    }
}
