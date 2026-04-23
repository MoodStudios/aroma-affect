package com.ovrtechnology.websocket;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder(toBuilder = true)
@With
public class WebSocketConfig {

    public static final WebSocketConfig DEFAULT = WebSocketConfig.builder().build();

    @Builder.Default private final String host = "127.0.0.1";

    @Builder.Default private final int port = 8080;

    @Builder.Default private final boolean secure = false;

    @Builder.Default private final String path = "";

    @Builder.Default private final boolean autoConnect = true;

    @Builder.Default private final boolean autoReconnect = true;

    @Builder.Default private final long initialReconnectDelayMs = 1_000L;

    @Builder.Default private final long maxReconnectDelayMs = 30_000L;

    @Builder.Default private final double reconnectBackoffMultiplier = 2.0;

    @Builder.Default private final int maxReconnectAttempts = -1;

    @Builder.Default private final long healthCheckIntervalMs = 5_000L;

    @Builder.Default private final long connectionTimeoutMs = 10_000L;

    @Builder.Default private final boolean debugLogging = false;

    public String getUri() {
        StringBuilder sb = new StringBuilder();
        sb.append(secure ? "wss://" : "ws://");
        sb.append(host);
        sb.append(":");
        sb.append(port);
        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/")) {
                sb.append("/");
            }
            sb.append(path);
        }
        return sb.toString();
    }

    public static WebSocketConfig of(String host, int port) {
        return WebSocketConfig.builder().host(host).port(port).build();
    }

    public static WebSocketConfig fromUri(String uri) {
        try {
            java.net.URI parsed = java.net.URI.create(uri);
            return WebSocketConfig.builder()
                    .host(parsed.getHost() != null ? parsed.getHost() : "localhost")
                    .port(parsed.getPort() > 0 ? parsed.getPort() : 8080)
                    .secure("wss".equalsIgnoreCase(parsed.getScheme()))
                    .path(parsed.getPath() != null ? parsed.getPath() : "")
                    .build();
        } catch (Exception e) {
            return DEFAULT;
        }
    }

    @Override
    public String toString() {
        return "WebSocketConfig{uri="
                + getUri()
                + ", autoConnect="
                + autoConnect
                + ", autoReconnect="
                + autoReconnect
                + "}";
    }
}
