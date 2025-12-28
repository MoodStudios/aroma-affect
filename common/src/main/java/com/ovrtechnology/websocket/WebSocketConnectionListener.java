package com.ovrtechnology.websocket;

/**
 * Listener interface for WebSocket connection state changes.
 * 
 * <p>Implementations can react to connection lifecycle events such as
 * successful connections, disconnections, and errors.</p>
 * 
 * <h3>Example:</h3>
 * <pre>{@code
 * client.addConnectionListener(new WebSocketConnectionListener() {
 *     @Override
 *     public void onConnected() {
 *         LOGGER.info("Connected to OVR hardware!");
 *     }
 *     
 *     @Override
 *     public void onDisconnected(String reason, boolean wasClean) {
 *         LOGGER.warn("Disconnected from OVR: {}", reason);
 *     }
 * });
 * }</pre>
 */
public interface WebSocketConnectionListener {
    
    /**
     * Called when a connection is successfully established.
     * This is called on Minecraft's main thread.
     */
    default void onConnected() {}
    
    /**
     * Called when the connection state changes.
     * This is called on Minecraft's main thread.
     * 
     * @param oldState the previous state
     * @param newState the new state
     */
    default void onStateChanged(ConnectionState oldState, ConnectionState newState) {}
    
    /**
     * Called when the connection is closed.
     * This is called on Minecraft's main thread.
     * 
     * @param reason a human-readable reason for the disconnection
     * @param wasClean true if this was a clean shutdown, false if unexpected
     */
    default void onDisconnected(String reason, boolean wasClean) {}
    
    /**
     * Called when a connection error occurs.
     * This is called on Minecraft's main thread.
     * 
     * @param error the error that occurred
     */
    default void onError(Throwable error) {}
    
    /**
     * Called when a reconnection attempt is about to start.
     * This is called on Minecraft's main thread.
     * 
     * @param attemptNumber the attempt number (1-based)
     * @param delayMs the delay before this attempt in milliseconds
     */
    default void onReconnecting(int attemptNumber, long delayMs) {}
}

