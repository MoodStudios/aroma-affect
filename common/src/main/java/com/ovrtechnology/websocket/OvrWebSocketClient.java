package com.ovrtechnology.websocket;

import com.ovrtechnology.AromaCraft;
import dev.architectury.event.events.client.ClientTickEvent;
import lombok.Getter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebSocket client for OVR hardware integration.
 * 
 * <p>
 * This client manages the WebSocket connection to OVR's scent hardware bridge.
 * It handles connection lifecycle, automatic reconnection, health monitoring,
 * and ensures callbacks are executed on Minecraft's main thread.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 * <li>Non-blocking connection that doesn't delay game startup</li>
 * <li>Automatic reconnection with exponential backoff</li>
 * <li>Health monitoring with ping/pong</li>
 * <li>Thread-safe message sending</li>
 * <li>Main thread callback execution</li>
 * </ul>
 * 
 * <h3>Usage:</h3>
 * 
 * <pre>{@code
 * OvrWebSocketClient client = OvrWebSocketClient.getInstance();
 * 
 * // Send a scent trigger
 * client.send(WebSocketMessage.scent("lavender", 0.8));
 * 
 * // Register message handler
 * client.addMessageHandler(msg -> {
 *     // Handle on main thread
 * });
 * }</pre>
 */
public final class OvrWebSocketClient implements WebSocket.Listener {

    private static final OvrWebSocketClient INSTANCE = new OvrWebSocketClient();

    // ========================================
    // Configuration
    // ========================================

    @Getter
    private volatile WebSocketConfig config = WebSocketConfig.DEFAULT;

    // ========================================
    // Connection state
    // ========================================

    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private volatile WebSocket webSocket;
    private final HttpClient httpClient;

    // ========================================
    // Reconnection tracking
    // ========================================

    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicLong currentReconnectDelay = new AtomicLong(0);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    // ========================================
    // Threading
    // ========================================

    private final ScheduledExecutorService scheduler;
    private final Queue<Runnable> mainThreadQueue = new ConcurrentLinkedQueue<>();
    private ScheduledFuture<?> healthCheckTask;
    private ScheduledFuture<?> reconnectTask;

    // ========================================
    // Message handling
    // ========================================

    private final List<WebSocketMessageHandler> messageHandlers = new CopyOnWriteArrayList<>();
    private final List<WebSocketConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final StringBuilder messageBuffer = new StringBuilder();

    // ========================================
    // Health monitoring
    // ========================================

    private final AtomicLong lastPongTime = new AtomicLong(0);
    private final AtomicBoolean awaitingPong = new AtomicBoolean(false);
    private final AtomicLong lastPingTime = new AtomicLong(0);

    // ========================================
    // Initialization flag
    // ========================================

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private OvrWebSocketClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(WebSocketConfig.DEFAULT.getConnectionTimeoutMs()))
                .build();

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AromaCraft-WebSocket");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Gets the singleton instance.
     * 
     * @return the WebSocket client instance
     */
    public static OvrWebSocketClient getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the WebSocket client.
     * Should be called during client-side mod initialization.
     * 
     * <p>
     * This registers tick handlers for main thread execution and
     * optionally starts the connection if autoConnect is enabled.
     * </p>
     */
    public static void init() {
        init(WebSocketConfig.DEFAULT);
    }

    /**
     * Initializes the WebSocket client with custom configuration.
     * 
     * @param config the configuration to use
     */
    public static void init(WebSocketConfig config) {
        if (INSTANCE.initialized.getAndSet(true)) {
            AromaCraft.LOGGER.warn("OvrWebSocketClient.init() called multiple times!");
            return;
        }

        INSTANCE.config = config;

        AromaCraft.LOGGER.info("Initializing OVR WebSocket client...");
        AromaCraft.LOGGER.info("  Target: {}", config.getUri());
        AromaCraft.LOGGER.info("  Auto-connect: {}", config.isAutoConnect());
        AromaCraft.LOGGER.info("  Auto-reconnect: {}", config.isAutoReconnect());

        // Register tick handler for main thread execution
        ClientTickEvent.CLIENT_POST.register(instance -> {
            INSTANCE.processMainThreadQueue();
        });

        // Start connection if auto-connect is enabled
        if (config.isAutoConnect()) {
            INSTANCE.connectAsync();
        }

        AromaCraft.LOGGER.info("OVR WebSocket client initialized");
    }

    /**
     * Updates the configuration and reconnects if necessary.
     * 
     * @param newConfig the new configuration
     */
    public void updateConfig(WebSocketConfig newConfig) {
        WebSocketConfig oldConfig = this.config;
        this.config = newConfig;

        // If URI changed and we're connected, reconnect
        if (!oldConfig.getUri().equals(newConfig.getUri())) {
            if (state.get().isActiveOrPending()) {
                AromaCraft.LOGGER.info("WebSocket URI changed, reconnecting...");
                disconnect();
                if (newConfig.isAutoConnect()) {
                    connectAsync();
                }
            }
        }
    }

    // ========================================
    // Connection management
    // ========================================

    /**
     * Initiates an asynchronous connection to the WebSocket server.
     * 
     * <p>
     * This method is non-blocking. Connection status can be monitored
     * via {@link #getState()} or by registering a
     * {@link WebSocketConnectionListener}.
     * </p>
     * 
     * @return a future that completes when the connection attempt finishes
     */
    public CompletableFuture<Boolean> connectAsync() {
        ConnectionState currentState = state.get();
        if (currentState == ConnectionState.CONNECTING || currentState == ConnectionState.CONNECTED) {
            if (config.isDebugLogging()) {
                AromaCraft.LOGGER.debug("Already {} - ignoring connect request", currentState);
            }
            return CompletableFuture.completedFuture(currentState == ConnectionState.CONNECTED);
        }

        setState(ConnectionState.CONNECTING);

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (config.isDebugLogging()) {
                    AromaCraft.LOGGER.debug("Connecting to {}...", config.getUri());
                }

                URI uri = URI.create(config.getUri());

                WebSocket ws = httpClient.newWebSocketBuilder()
                        .connectTimeout(Duration.ofMillis(config.getConnectionTimeoutMs()))
                        .buildAsync(uri, this)
                        .get(config.getConnectionTimeoutMs(), TimeUnit.MILLISECONDS);

                this.webSocket = ws;
                setState(ConnectionState.CONNECTED);

                // Reset reconnect counters on successful connection
                reconnectAttempts.set(0);
                currentReconnectDelay.set(config.getInitialReconnectDelayMs());

                AromaCraft.LOGGER.info("=== OVR WebSocket CONNECTED to {} ===", config.getUri());

                // Start health monitoring (can be disabled for debugging by setting interval to 0)
                if (config.getHealthCheckIntervalMs() > 0) {
                    startHealthMonitoring();
                } else {
                    AromaCraft.LOGGER.info("Health monitoring disabled (interval=0)");
                }

                // Notify listeners on main thread
                executeOnMainThread(() -> {
                    for (WebSocketConnectionListener listener : connectionListeners) {
                        try {
                            listener.onConnected();
                        } catch (Exception e) {
                            AromaCraft.LOGGER.error("Error in connection listener", e);
                        }
                    }
                });

                AromaCraft.LOGGER.info("OVR WebSocket ready to send/receive messages");
                return true;

            } catch (Exception e) {
                handleConnectionFailure(e);
                return false;
            }
        }, scheduler);
    }

    /**
     * Disconnects from the WebSocket server.
     * 
     * <p>
     * This stops any pending reconnection attempts and performs a clean shutdown.
     * </p>
     */
    public void disconnect() {
        disconnect("Manual disconnect", true);
    }

    /**
     * Disconnects with a specific reason.
     * 
     * @param reason          the disconnect reason
     * @param cancelReconnect whether to cancel pending reconnection
     */
    public void disconnect(String reason, boolean cancelReconnect) {
        if (cancelReconnect) {
            cancelReconnectTask();
        }

        stopHealthMonitoring();

        WebSocket ws = this.webSocket;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, reason);
            } catch (Exception e) {
                if (config.isDebugLogging()) {
                    AromaCraft.LOGGER.debug("Error during disconnect: {}", e.getMessage());
                }
            }
            this.webSocket = null;
        }

        setState(ConnectionState.DISCONNECTED);

        final String finalReason = reason;
        executeOnMainThread(() -> {
            for (WebSocketConnectionListener listener : connectionListeners) {
                try {
                    listener.onDisconnected(finalReason, true);
                } catch (Exception e) {
                    AromaCraft.LOGGER.error("Error in connection listener", e);
                }
            }
        });

        AromaCraft.LOGGER.info("Disconnected from OVR WebSocket: {}", reason);
    }

    /**
     * Shuts down the client completely.
     * Should be called when the mod is being unloaded.
     */
    public void shutdown() {
        AromaCraft.LOGGER.info("Shutting down OVR WebSocket client...");

        disconnect("Shutdown", true);

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        messageHandlers.clear();
        connectionListeners.clear();
        mainThreadQueue.clear();
    }

    // ========================================
    // Message sending
    // ========================================

    /**
     * Sends a message to the WebSocket server.
     * 
     * <p>
     * If not connected, the message is silently dropped (with debug logging).
     * </p>
     * 
     * @param message the message to send
     * @return true if the message was queued for sending, false if not connected
     */
    public boolean send(WebSocketMessage message) {
        if (!state.get().canSendMessages()) {
            if (config.isDebugLogging()) {
                AromaCraft.LOGGER.debug("Cannot send message - not connected. State: {}", state.get());
            }
            return false;
        }

        WebSocket ws = this.webSocket;
        if (ws == null) {
            return false;
        }

        try {
            String rawText = message.toRawText();
            ws.sendText(rawText, true);

            // Always log sent messages at info level for debugging OVR communication
            AromaCraft.LOGGER.info("WebSocket SENT: {}", rawText);
            if (config.isDebugLogging()) {
                AromaCraft.LOGGER.debug("Sent message object: {}", message);
            }

            return true;
        } catch (Exception e) {
            AromaCraft.LOGGER.error("Error sending message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sends a raw text message.
     * 
     * @param text the text to send
     * @return true if sent successfully
     */
    public boolean sendRaw(String text) {
        return send(new WebSocketMessage("raw", text));
    }

    // ========================================
    // Handler registration
    // ========================================

    /**
     * Adds a message handler.
     * Handlers are called on Minecraft's main thread.
     * 
     * @param handler the handler to add
     */
    public void addMessageHandler(WebSocketMessageHandler handler) {
        messageHandlers.add(handler);
    }

    /**
     * Removes a message handler.
     * 
     * @param handler the handler to remove
     */
    public void removeMessageHandler(WebSocketMessageHandler handler) {
        messageHandlers.remove(handler);
    }

    /**
     * Adds a connection listener.
     * Listeners are called on Minecraft's main thread.
     * 
     * @param listener the listener to add
     */
    public void addConnectionListener(WebSocketConnectionListener listener) {
        connectionListeners.add(listener);
    }

    /**
     * Removes a connection listener.
     * 
     * @param listener the listener to remove
     */
    public void removeConnectionListener(WebSocketConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    // ========================================
    // State accessors
    // ========================================

    /**
     * Gets the current connection state.
     * 
     * @return the current state
     */
    public ConnectionState getState() {
        return state.get();
    }

    /**
     * Checks if currently connected.
     * 
     * @return true if connected
     */
    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED;
    }

    /**
     * Gets the number of reconnection attempts since last successful connection.
     * 
     * @return the attempt count
     */
    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }

    // ========================================
    // WebSocket.Listener implementation
    // ========================================

    @Override
    public void onOpen(WebSocket webSocket) {
        if (config.isDebugLogging()) {
            AromaCraft.LOGGER.debug("WebSocket opened");
        }
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        messageBuffer.append(data);

        if (last) {
            String fullMessage = messageBuffer.toString();
            messageBuffer.setLength(0);

            WebSocketMessage message = WebSocketMessage.fromRawText(fullMessage);

            if (config.isDebugLogging()) {
                AromaCraft.LOGGER.debug("Received: {}", message);
            }

            // Handle ping/pong internally
            if (message.isPing()) {
                send(WebSocketMessage.pong());
            } else if (message.isPong()) {
                lastPongTime.set(System.currentTimeMillis());
                awaitingPong.set(false);
            } else {
                // Dispatch to handlers on main thread
                executeOnMainThread(() -> {
                    for (WebSocketMessageHandler handler : messageHandlers) {
                        try {
                            handler.onMessage(message);
                        } catch (Exception e) {
                            AromaCraft.LOGGER.error("Error in message handler", e);
                        }
                    }
                });
            }
        }

        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        // Binary messages are not expected, but handle gracefully
        if (config.isDebugLogging()) {
            AromaCraft.LOGGER.debug("Received binary message ({} bytes)", data.remaining());
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        if (config.isDebugLogging()) {
            AromaCraft.LOGGER.debug("Received WebSocket ping");
        }
        // java.net.http.WebSocket typically auto-responds to ping frames, but sending
        // an explicit pong can improve compatibility with some servers.
        try {
            webSocket.sendPong(message);
        } catch (Exception e) {
            if (config.isDebugLogging()) {
                AromaCraft.LOGGER.debug("Failed to send WebSocket pong: {}", e.getMessage());
            }
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        lastPongTime.set(System.currentTimeMillis());
        awaitingPong.set(false);
        if (config.isDebugLogging()) {
            AromaCraft.LOGGER.debug("Received WebSocket pong");
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        AromaCraft.LOGGER.info("WebSocket closed: {} (code: {})", reason, statusCode);

        this.webSocket = null;
        stopHealthMonitoring();

        boolean wasClean = statusCode == WebSocket.NORMAL_CLOSURE;

        executeOnMainThread(() -> {
            for (WebSocketConnectionListener listener : connectionListeners) {
                try {
                    listener.onDisconnected(reason, wasClean);
                } catch (Exception e) {
                    AromaCraft.LOGGER.error("Error in connection listener", e);
                }
            }
        });

        if (!wasClean && config.isAutoReconnect()) {
            scheduleReconnect();
        } else {
            setState(ConnectionState.DISCONNECTED);
        }

        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        AromaCraft.LOGGER.error("WebSocket error: {}", error.getMessage());

        executeOnMainThread(() -> {
            for (WebSocketConnectionListener listener : connectionListeners) {
                try {
                    listener.onError(error);
                } catch (Exception e) {
                    AromaCraft.LOGGER.error("Error in connection listener", e);
                }
            }
        });
    }

    // ========================================
    // Reconnection logic
    // ========================================

    private void handleConnectionFailure(Exception e) {
        setState(ConnectionState.CONNECTION_FAILED);

        // Only log once on first failure to avoid spam
        if (reconnectAttempts.get() == 0) {
            AromaCraft.LOGGER.info("OVR WebSocket server is offline, will keep trying to reconnect...");
        }

        if (config.isDebugLogging()) {
            String errorMsg = e.getMessage();
            if (e.getCause() != null) {
                errorMsg = e.getCause().getMessage();
            }
            AromaCraft.LOGGER.debug("Connection failure details: {}", errorMsg);
        }

        executeOnMainThread(() -> {
            for (WebSocketConnectionListener listener : connectionListeners) {
                try {
                    listener.onError(e);
                } catch (Exception ex) {
                    AromaCraft.LOGGER.error("Error in connection listener", ex);
                }
            }
        });

        if (config.isAutoReconnect()) {
            scheduleReconnect();
        } else {
            setState(ConnectionState.DISCONNECTED);
        }
    }

    private void scheduleReconnect() {
        if (reconnectScheduled.getAndSet(true)) {
            return; // Already scheduled
        }

        int attempts = reconnectAttempts.incrementAndGet();
        int maxAttempts = config.getMaxReconnectAttempts();

        if (maxAttempts > 0 && attempts > maxAttempts) {
            AromaCraft.LOGGER.warn("Max reconnection attempts ({}) reached. Giving up.", maxAttempts);
            setState(ConnectionState.DISCONNECTED);
            reconnectScheduled.set(false);
            return;
        }

        // Calculate delay with exponential backoff
        long delay = currentReconnectDelay.get();
        if (delay == 0) {
            delay = config.getInitialReconnectDelayMs();
        }

        // Apply jitter (±10%)
        double jitter = 0.9 + (Math.random() * 0.2);
        delay = (long) (delay * jitter);

        // Cap at max delay
        delay = Math.min(delay, config.getMaxReconnectDelayMs());

        final long finalDelay = delay;

        if (config.isDebugLogging()) {
            AromaCraft.LOGGER.debug("Reconnecting in {} ms (attempt {})...", delay, attempts);
        }

        setState(ConnectionState.RECONNECTING);

        // Notify listeners
        executeOnMainThread(() -> {
            for (WebSocketConnectionListener listener : connectionListeners) {
                try {
                    listener.onReconnecting(attempts, finalDelay);
                } catch (Exception e) {
                    AromaCraft.LOGGER.error("Error in connection listener", e);
                }
            }
        });

        // Schedule reconnection
        reconnectTask = scheduler.schedule(() -> {
            reconnectScheduled.set(false);

            // Increase delay for next attempt
            long nextDelay = (long) (currentReconnectDelay.get() * config.getReconnectBackoffMultiplier());
            currentReconnectDelay.set(Math.min(nextDelay, config.getMaxReconnectDelayMs()));

            connectAsync();
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void cancelReconnectTask() {
        reconnectScheduled.set(false);
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
    }

    // ========================================
    // Health monitoring
    // ========================================

    private void startHealthMonitoring() {
        stopHealthMonitoring();

        // Initialize timestamps to avoid immediate false timeouts
        long now = System.currentTimeMillis();
        lastPongTime.set(now);
        lastPingTime.set(now);
        awaitingPong.set(false);

        healthCheckTask = scheduler.scheduleAtFixedRate(
                this::performHealthCheck,
                config.getHealthCheckIntervalMs(),
                config.getHealthCheckIntervalMs(),
                TimeUnit.MILLISECONDS);

        if (config.isDebugLogging()) {
            AromaCraft.LOGGER.debug("Health monitoring started (interval: {} ms)", config.getHealthCheckIntervalMs());
        }
    }

    private void stopHealthMonitoring() {
        if (healthCheckTask != null) {
            healthCheckTask.cancel(false);
            healthCheckTask = null;
        }
        awaitingPong.set(false);
        lastPingTime.set(0);
    }

    private void performHealthCheck() {
        if (state.get() != ConnectionState.CONNECTED) {
            return;
        }

        // Check if we were waiting for a pong that never came
        if (awaitingPong.get()) {
            long elapsed = System.currentTimeMillis() - lastPingTime.get();
            if (elapsed > config.getHealthCheckIntervalMs() * 2) {
                AromaCraft.LOGGER.warn("Health check failed: No pong received in {} ms", elapsed);
                disconnect("Health check timeout", false);
                if (config.isAutoReconnect()) {
                    scheduleReconnect();
                }
                return;
            }
        }

        // Send a protocol-level WebSocket ping frame.
        // This avoids relying on custom text ping/pong messages which the server may
        // not support.
        WebSocket ws = this.webSocket;
        if (ws == null) {
            return;
        }

        awaitingPong.set(true);
        lastPingTime.set(System.currentTimeMillis());
        try {
            ws.sendPing(ByteBuffer.allocate(0));
        } catch (Exception e) {
            AromaCraft.LOGGER.warn("Health check ping failed: {}", e.getMessage());
        }

        if (config.isDebugLogging()) {
            AromaCraft.LOGGER.debug("Health check: ping sent");
        }
    }

    // ========================================
    // Main thread execution
    // ========================================

    /**
     * Queues a task to be executed on Minecraft's main thread.
     * 
     * @param task the task to execute
     */
    public void executeOnMainThread(Runnable task) {
        mainThreadQueue.offer(task);
    }

    /**
     * Processes queued main thread tasks.
     * Called from the client tick handler.
     */
    private void processMainThreadQueue() {
        // Process a limited number of tasks per tick to avoid lag
        int processed = 0;
        Runnable task;
        while (processed < 10 && (task = mainThreadQueue.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                AromaCraft.LOGGER.error("Error executing main thread task", e);
            }
            processed++;
        }
    }

    // ========================================
    // State management
    // ========================================

    private void setState(ConnectionState newState) {
        ConnectionState oldState = state.getAndSet(newState);

        if (oldState != newState) {
            if (config.isDebugLogging()) {
                AromaCraft.LOGGER.debug("State changed: {} -> {}", oldState, newState);
            }

            executeOnMainThread(() -> {
                for (WebSocketConnectionListener listener : connectionListeners) {
                    try {
                        listener.onStateChanged(oldState, newState);
                    } catch (Exception e) {
                        AromaCraft.LOGGER.error("Error in connection listener", e);
                    }
                }
            });
        }
    }
}
