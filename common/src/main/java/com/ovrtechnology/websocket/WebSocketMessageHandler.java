package com.ovrtechnology.websocket;

/**
 * Handler interface for processing incoming WebSocket messages.
 * 
 * <p>Implementations of this interface receive messages after they have been
 * parsed and are called on Minecraft's main thread for thread-safe access
 * to game state.</p>
 * 
 * <h3>Example Implementation:</h3>
 * <pre>{@code
 * client.addMessageHandler(message -> {
 *     if (message.isScent()) {
 *         // Trigger visual/audio feedback for scent
 *         ScentEffectRenderer.play(message.getPayload());
 *     }
 * });
 * }</pre>
 * 
 * @see OvrWebSocketClient#addMessageHandler(WebSocketMessageHandler)
 */
@FunctionalInterface
public interface WebSocketMessageHandler {
    
    /**
     * Handles an incoming WebSocket message.
     * 
     * <p>This method is called on Minecraft's main thread, so it's safe to
     * interact with game state, render effects, or modify entities.</p>
     * 
     * @param message the received message
     */
    void onMessage(WebSocketMessage message);
}

