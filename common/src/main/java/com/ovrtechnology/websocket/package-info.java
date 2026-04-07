/**
 * WebSocket client module for OVR hardware integration.
 * 
 * <h2>Overview</h2>
 * <p>This package provides a robust WebSocket client for communicating with OVR's
 * scent hardware bridge. The connection is entirely optional - the mod works
 * perfectly without it, but when connected, it enables scent hardware integration.</p>
 * 
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.ovrtechnology.websocket.WebSocketClient} - Main client managing connection lifecycle</li>
 *   <li>{@link com.ovrtechnology.websocket.WebSocketConfig} - Connection configuration (host, port, etc.)</li>
 *   <li>{@link com.ovrtechnology.websocket.WebSocketMessage} - Message wrapper for send/receive</li>
 *   <li>{@link com.ovrtechnology.websocket.ConnectionState} - Connection state enum</li>
 * </ul>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Non-blocking connection that doesn't delay Minecraft startup</li>
 *   <li>Automatic reconnection with exponential backoff</li>
 *   <li>Health monitoring in a separate thread</li>
 *   <li>Main thread callback execution for safe Minecraft API access</li>
 *   <li>Graceful degradation when server is unavailable</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Get the client instance (initialized during mod startup)
 * OvrWebSocketClient client = OvrWebSocketClient.getInstance();
 * 
 * // Send a scent trigger
 * client.send(new WebSocketMessage("scent", "{\"scentId\": \"lavender\", \"intensity\": 0.8}"));
 * 
 * // Register a message handler (called on main thread)
 * client.onMessage(message -> {
 *     // Handle incoming message safely on Minecraft's main thread
 * });
 * }</pre>
 * 
 * <h2>Configuration</h2>
 * <p>Default connection: ws://localhost:8080</p>
 * <p>Configuration can be modified through the mod's config system.</p>
 * 
 * @since 0.0.1-experimental.1
 */
package com.ovrtechnology.websocket;


