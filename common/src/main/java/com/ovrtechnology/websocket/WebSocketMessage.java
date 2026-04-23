package com.ovrtechnology.websocket;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class WebSocketMessage {

    private final String id;

    private final String type;

    private final String payload;

    private final Instant timestamp;

    private final boolean outgoing;

    public WebSocketMessage(String type, String payload) {
        this(UUID.randomUUID().toString(), type, payload, Instant.now(), true);
    }

    public WebSocketMessage(String type, String payload, boolean outgoing) {
        this(UUID.randomUUID().toString(), type, payload, Instant.now(), outgoing);
    }

    public WebSocketMessage(
            String id, String type, String payload, Instant timestamp, boolean outgoing) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.payload = payload != null ? payload : "";
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.outgoing = outgoing;
    }

    public static WebSocketMessage ping() {
        return new WebSocketMessage("ping", "");
    }

    public static WebSocketMessage pong() {
        return new WebSocketMessage("pong", "");
    }

    public static WebSocketMessage scent(String scentId, double intensity) {

        String payload =
                String.format(
                        Locale.ROOT,
                        "{\"scentId\":\"%s\",\"intensity\":%.2f}",
                        scentId,
                        Math.max(0.0, Math.min(1.0, intensity)));
        return new WebSocketMessage("scent", payload);
    }

    public static WebSocketMessage stop() {
        return new WebSocketMessage("stop", "");
    }

    public static WebSocketMessage playScent(String scentName, double intensity) {

        double clampedIntensity = Math.max(0.0, Math.min(1.0, intensity));
        String payload =
                String.format("{\"odor\":\"%s\",\"intensity\":%.2f}", scentName, clampedIntensity);

        return new WebSocketMessage("raw", payload);
    }

    public static WebSocketMessage playScent(String scentName) {
        return playScent(scentName, 0.5);
    }

    public static WebSocketMessage fromRawText(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return new WebSocketMessage("unknown", "", false);
        }

        int colonIndex = rawText.indexOf(':');
        if (colonIndex > 0) {
            String type = rawText.substring(0, colonIndex).trim();
            String payload = rawText.substring(colonIndex + 1).trim();
            return new WebSocketMessage(type, payload, false);
        }

        return new WebSocketMessage(rawText.trim(), "", false);
    }

    public String toRawText() {

        if ("raw".equals(type)) {
            return payload != null ? payload : "";
        }

        if (payload == null || payload.isEmpty()) {
            return type;
        }
        return type + ":" + payload;
    }

    public boolean isPing() {
        return "ping".equalsIgnoreCase(type);
    }

    public boolean isPong() {
        return "pong".equalsIgnoreCase(type);
    }

    public boolean isScent() {
        return "scent".equalsIgnoreCase(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketMessage that = (WebSocketMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WebSocketMessage{"
                + "id='"
                + id
                + '\''
                + ", type='"
                + type
                + '\''
                + ", payload='"
                + (payload.length() > 50 ? payload.substring(0, 50) + "..." : payload)
                + '\''
                + ", outgoing="
                + outgoing
                + '}';
    }
}
