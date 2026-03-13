package com.ovrtechnology.tutorial;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;

/**
 * Keeps the tutorial world at daytime with clear weather.
 * <p>
 * When the tutorial GameRule is active, this handler:
 * <ul>
 *   <li>Resets the time to noon whenever it reaches sunset</li>
 *   <li>Clears any rain, snow, or thunderstorms</li>
 * </ul>
 */
public final class TutorialDaylightHandler {

    private static boolean initialized = false;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 100; // Check every 5 seconds

    /**
     * Minecraft time constants:
     * - 0 = sunrise (6:00 AM)
     * - 6000 = noon (12:00 PM)
     * - 12000 = sunset (6:00 PM)
     * - 18000 = midnight (12:00 AM)
     */
    private static final long TIME_NOON = 6000L;
    private static final long TIME_SUNSET = 12000L;

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
                    AromaAffect.LOGGER.debug("Tutorial: Reset time to noon (was {})", dayTime);
                }

                // Clear weather (rain, snow, thunderstorms)
                if (level.isRaining() || level.isThundering()) {
                    level.setWeatherParameters(
                            6000,  // clearWeatherTime (ticks of clear weather)
                            0,     // rainTime
                            false, // raining
                            false  // thundering
                    );
                    AromaAffect.LOGGER.debug("Tutorial: Cleared weather");
                }
            }
        });

        AromaAffect.LOGGER.debug("Tutorial daylight handler initialized");
    }
}
