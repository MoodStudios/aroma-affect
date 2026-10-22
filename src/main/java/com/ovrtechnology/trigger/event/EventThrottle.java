package com.ovrtechnology.trigger.event;

import com.ovrtechnology.AromaAffect;
import java.util.ArrayDeque;
import java.util.Deque;

public final class EventThrottle {

    private static final long WINDOW_MS = 60_000L;

    private static final Deque<Long> recentFires = new ArrayDeque<>();

    private EventThrottle() {}

    public static synchronized boolean tryConsume() {
        long now = System.currentTimeMillis();
        cleanup(now);
        int limit = EventTriggersConfig.getInstance().getGlobalThrottlePerMinute();
        if (limit <= 0) {
            recentFires.add(now);
            return true;
        }
        if (recentFires.size() >= limit) {
            AromaAffect.LOGGER.debug(
                    "[Events] global throttle reached ({} / min), suppressing", limit);
            return false;
        }
        recentFires.add(now);
        return true;
    }

    private static void cleanup(long now) {
        while (!recentFires.isEmpty() && now - recentFires.peekFirst() > WINDOW_MS) {
            recentFires.pollFirst();
        }
    }

    public static synchronized int getCurrentCount() {
        cleanup(System.currentTimeMillis());
        return recentFires.size();
    }

    public static synchronized void reset() {
        recentFires.clear();
    }
}
