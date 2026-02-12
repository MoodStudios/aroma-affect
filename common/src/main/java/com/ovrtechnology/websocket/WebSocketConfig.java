package com.ovrtechnology.websocket;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

/**
 * Configuration for the OVR WebSocket connection.
 * 
 * <p>This class is immutable and uses a builder pattern for construction.
 * All values have sensible defaults for connecting to OVR's local bridge.</p>
 * 
 * <h3>Default Configuration:</h3>
 * <ul>
 *   <li>Host: localhost</li>
 *   <li>Port: 8080</li>
 *   <li>Protocol: ws (not wss)</li>
 *   <li>Auto-connect: enabled</li>
 *   <li>Auto-reconnect: enabled</li>
 *   <li>Initial reconnect delay: 1 second</li>
 *   <li>Max reconnect delay: 30 seconds</li>
 *   <li>Health check interval: 5 seconds</li>
 * </ul>
 */
@Getter
@Builder(toBuilder = true)
@With
public class WebSocketConfig {
    
    /**
     * Default configuration instance.
     */
    public static final WebSocketConfig DEFAULT = WebSocketConfig.builder().build();
    
    /**
     * The WebSocket server host.
     */
    @Builder.Default
    private final String host = "127.0.0.1";
    
    /**
     * The WebSocket server port.
     */
    @Builder.Default
    private final int port = 8080;
    
    /**
     * Whether to use secure WebSocket (wss) instead of ws.
     */
    @Builder.Default
    private final boolean secure = false;
    
    /**
     * The WebSocket endpoint path (appended to the URL).
     */
    @Builder.Default
    private final String path = "";
    
    /**
     * Whether to automatically attempt connection on initialization.
     */
    @Builder.Default
    private final boolean autoConnect = true;
    
    /**
     * Whether to automatically reconnect after connection loss.
     */
    @Builder.Default
    private final boolean autoReconnect = true;
    
    /**
     * Initial delay before first reconnection attempt, in milliseconds.
     */
    @Builder.Default
    private final long initialReconnectDelayMs = 1_000L;
    
    /**
     * Maximum delay between reconnection attempts, in milliseconds.
     * The delay grows exponentially up to this maximum.
     */
    @Builder.Default
    private final long maxReconnectDelayMs = 30_000L;
    
    /**
     * Backoff multiplier for exponential reconnection delay.
     */
    @Builder.Default
    private final double reconnectBackoffMultiplier = 2.0;
    
    /**
     * Maximum number of reconnection attempts before giving up.
     * Set to -1 for unlimited attempts.
     */
    @Builder.Default
    private final int maxReconnectAttempts = -1;
    
    /**
     * Interval between health checks when connected, in milliseconds.
     */
    @Builder.Default
    private final long healthCheckIntervalMs = 5_000L;
    
    /**
     * Timeout for connection attempts, in milliseconds.
     */
    @Builder.Default
    private final long connectionTimeoutMs = 10_000L;
    
    /**
     * Whether to enable verbose debug logging.
     */
    @Builder.Default
    private final boolean debugLogging = false;
    
    /**
     * Constructs the full WebSocket URI from the configuration.
     * 
     * @return the WebSocket URI string (e.g., "ws://localhost:8080/path")
     */
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
    
    /**
     * Creates a configuration for a specific host and port.
     * 
     * @param host the server host
     * @param port the server port
     * @return a new configuration
     */
    public static WebSocketConfig of(String host, int port) {
        return WebSocketConfig.builder()
                .host(host)
                .port(port)
                .build();
    }
    
    /**
     * Creates a configuration from a full URI.
     * 
     * @param uri the WebSocket URI (e.g., "ws://localhost:8080/path")
     * @return a new configuration, or DEFAULT if parsing fails
     */
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
        return "WebSocketConfig{uri=" + getUri() + 
               ", autoConnect=" + autoConnect + 
               ", autoReconnect=" + autoReconnect + "}";
    }
}

