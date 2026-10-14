package com.ovrtechnology.websocket;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Decides how much to log for a repeating event, such as a connection failure.
 *
 * <p>The first event for a key, or an event with a different key than the previous one,
 * is logged in detail. Repeats of the same key are suppressed, except for one summary
 * line per interval that reports how many repeats were suppressed. This keeps the log
 * readable while the bridge is offline and retries run indefinitely.</p>
 *
 * <p>This class is thread-safe.</p>
 */
final class LogThrottle {

    /** What the caller should log for a recorded event. */
    enum Level {
        /** First event for this key: log the full details. */
        DETAILED,
        /** Repeat of the same key after the interval elapsed: log a summary. */
        SUMMARY,
        /** Repeat within the interval: log at debug level only. */
        SUPPRESSED
    }

    /**
     * Outcome of {@link #record(Object)}.
     *
     * @param level      how much to log
     * @param suppressed for {@link Level#SUMMARY}, the number of repeats suppressed
     *                   since the previous logged line; otherwise 0
     */
    record Decision(Level level, int suppressed) {}

    private final long intervalMs;
    private final LongSupplier clockMs;

    // Sentinel so that a null key is still treated as new on the first call
    private static final Object NO_KEY = new Object();

    private Object lastKey = NO_KEY;
    private long lastLoggedAtMs;
    private int suppressed;

    /**
     * Creates a throttle using the system clock.
     *
     * @param intervalMs minimum time between log lines for repeats of one key
     */
    LogThrottle(long intervalMs) {
        this(intervalMs, System::currentTimeMillis);
    }

    /**
     * Creates a throttle with an explicit clock.
     *
     * @param intervalMs minimum time between log lines for repeats of one key
     * @param clockMs    source of the current time in milliseconds
     */
    LogThrottle(long intervalMs, LongSupplier clockMs) {
        this.intervalMs = intervalMs;
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs");
    }

    /**
     * Records an event and returns how much of it to log.
     *
     * @param key identifies the kind of event, e.g. a {@link ConnectionFailure}
     * @return the logging decision
     */
    synchronized Decision record(Object key) {
        long now = clockMs.getAsLong();
        if (!Objects.equals(key, lastKey)) {
            lastKey = key;
            lastLoggedAtMs = now;
            suppressed = 0;
            return new Decision(Level.DETAILED, 0);
        }
        if (now - lastLoggedAtMs >= intervalMs) {
            int count = suppressed;
            lastLoggedAtMs = now;
            suppressed = 0;
            return new Decision(Level.SUMMARY, count);
        }
        suppressed++;
        return new Decision(Level.SUPPRESSED, 0);
    }

    /**
     * Forgets the previous event so the next one is logged in detail.
     */
    synchronized void reset() {
        lastKey = NO_KEY;
        lastLoggedAtMs = 0;
        suppressed = 0;
    }
}
