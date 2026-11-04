package com.ovrtechnology.websocket;

import com.ovrtechnology.websocket.LogThrottle.Decision;
import com.ovrtechnology.websocket.LogThrottle.Level;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link LogThrottle}.
 */
@DisplayName("LogThrottle")
class LogThrottleTest {

    private static final long INTERVAL_MS = 60_000;

    private final AtomicLong now = new AtomicLong(1_000_000);
    private LogThrottle throttle;

    @BeforeEach
    void setUp() {
        throttle = new LogThrottle(INTERVAL_MS, now::get);
    }

    @Test
    @DisplayName("first failure is DETAILED")
    void firstFailureIsDetailed() {
        assertThat(throttle.record(ConnectionFailure.REFUSED)).isEqualTo(new Decision(Level.DETAILED, 0));
    }

    @Test
    @DisplayName("repeats within the interval are SUPPRESSED")
    void repeatsWithinIntervalAreSuppressed() {
        throttle.record(ConnectionFailure.REFUSED);
        now.addAndGet(INTERVAL_MS - 1);
        assertThat(throttle.record(ConnectionFailure.REFUSED).level()).isEqualTo(Level.SUPPRESSED);
    }

    @Test
    @DisplayName("first repeat after the interval is a SUMMARY with the suppressed count")
    void summaryAfterInterval() {
        throttle.record(ConnectionFailure.REFUSED);
        for (int i = 0; i < 5; i++) {
            now.addAndGet(1_000);
            throttle.record(ConnectionFailure.REFUSED);
        }
        now.addAndGet(INTERVAL_MS);
        assertThat(throttle.record(ConnectionFailure.REFUSED)).isEqualTo(new Decision(Level.SUMMARY, 5));

        // The window restarts after a summary
        now.addAndGet(1_000);
        assertThat(throttle.record(ConnectionFailure.REFUSED).level()).isEqualTo(Level.SUPPRESSED);
    }

    @Test
    @DisplayName("a different failure category is DETAILED immediately")
    void categoryChangeIsDetailed() {
        throttle.record(ConnectionFailure.REFUSED);
        throttle.record(ConnectionFailure.REFUSED);
        assertThat(throttle.record(ConnectionFailure.TIMEOUT)).isEqualTo(new Decision(Level.DETAILED, 0));
    }

    @Test
    @DisplayName("a null key is DETAILED the first time")
    void nullKeyIsDetailedFirst() {
        assertThat(throttle.record(null).level()).isEqualTo(Level.DETAILED);
        assertThat(throttle.record(null).level()).isEqualTo(Level.SUPPRESSED);
    }

    @Test
    @DisplayName("reset makes the next failure DETAILED")
    void resetMakesNextDetailed() {
        throttle.record(ConnectionFailure.REFUSED);
        throttle.reset();
        assertThat(throttle.record(ConnectionFailure.REFUSED).level()).isEqualTo(Level.DETAILED);
    }
}
