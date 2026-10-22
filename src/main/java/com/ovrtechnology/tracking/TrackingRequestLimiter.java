package com.ovrtechnology.tracking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player throttle, accessed only on the server thread. Uses monotonic nanoseconds. */
public final class TrackingRequestLimiter {
    private final Map<UUID, Long> acceptedAt = new HashMap<>();
    private final long intervalNanos;

    public TrackingRequestLimiter(long intervalNanos) {
        if (intervalNanos < 0) throw new IllegalArgumentException("Negative request interval");
        this.intervalNanos = intervalNanos;
    }

    public boolean allow(UUID player, long now) {
        Long previous = acceptedAt.get(player);
        if (previous != null && now - previous < intervalNanos) return false;
        acceptedAt.put(player, now);
        return true;
    }

    public void remove(UUID player) { acceptedAt.remove(player); }
}
