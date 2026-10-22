package com.ovrtechnology.tracking;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TrackingRequestLimiterTest {
    @Test void repeatedRequestsDoNotExtendTheCooldown() {
        var limiter = new TrackingRequestLimiter(100);
        var player = UUID.randomUUID();
        assertThat(limiter.allow(player, 0)).isTrue();
        assertThat(limiter.allow(player, 99)).isFalse();
        assertThat(limiter.allow(player, 100)).isTrue();
        assertThat(limiter.allow(player, 101)).isFalse();
    }

    @Test void playersAndRefreshRequestsHaveIndependentBudgets() {
        var tracking = new TrackingRequestLimiter(100);
        var refresh = new TrackingRequestLimiter(100);
        var first = UUID.randomUUID();
        assertThat(refresh.allow(first, 10)).isTrue();
        assertThat(tracking.allow(first, 10)).isTrue();
        assertThat(tracking.allow(UUID.randomUUID(), 10)).isTrue();
        assertThat(tracking.allow(first, 11)).isFalse();
    }

    @Test void reconnectClearsOldRequestTimes() {
        var limiter = new TrackingRequestLimiter(100);
        var player = UUID.randomUUID();
        assertThat(limiter.allow(player, 50)).isTrue();
        limiter.remove(player);
        assertThat(limiter.allow(player, 51)).isTrue();
    }

    @Test void monotonicClockMayHaveANegativeOrigin() {
        var limiter = new TrackingRequestLimiter(100);
        var player = UUID.randomUUID();
        assertThat(limiter.allow(player, -200)).isTrue();
        assertThat(limiter.allow(player, -101)).isFalse();
        assertThat(limiter.allow(player, -100)).isTrue();
    }
}
