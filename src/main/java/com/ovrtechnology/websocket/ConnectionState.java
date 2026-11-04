package com.ovrtechnology.websocket;

/**
 * Represents the current state of the WebSocket connection.
 * 
 * <p>State transitions:</p>
 * <pre>
 * DISCONNECTED ─────┬──────► CONNECTING ──────► CONNECTED
 *       ▲           │              │                │
 *       │           │              ▼                ▼
 *       │           │         CONNECTION_FAILED    │
 *       │           │              │                │
 *       │           └──────────────┤                │
 *       │                          ▼                │
 *       └─────────────────── RECONNECTING ◄────────┘
 * </pre>
 */
public enum ConnectionState {
    
    /**
     * No active connection. This is the initial state and the state after
     * a manual disconnect or after maximum reconnection attempts are exhausted.
     */
    DISCONNECTED("Disconnected"),
    
    /**
     * Currently attempting to establish a connection.
     * This is a transient state that will transition to either CONNECTED or CONNECTION_FAILED.
     */
    CONNECTING("Connecting..."),
    
    /**
     * Successfully connected to the WebSocket server.
     * Messages can be sent and received in this state.
     */
    CONNECTED("Connected"),
    
    /**
     * Connection attempt failed.
     * This is a transient state before transitioning to RECONNECTING or DISCONNECTED.
     */
    CONNECTION_FAILED("Connection Failed"),
    
    /**
     * Waiting to reconnect after a connection failure or unexpected disconnection.
     * The backoff delay is applied in this state.
     */
    RECONNECTING("Reconnecting...");
    
    private final String displayName;
    
    ConnectionState(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets a human-readable display name for this state.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Checks if this state represents an active or pending connection.
     * 
     * @return true if connecting, connected, or reconnecting
     */
    public boolean isActiveOrPending() {
        return this == CONNECTING || this == CONNECTED || this == RECONNECTING;
    }
    
    /**
     * Checks if messages can be sent in this state.
     * 
     * @return true if connected
     */
    public boolean canSendMessages() {
        return this == CONNECTED;
    }
}

