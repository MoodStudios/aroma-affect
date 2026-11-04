package com.ovrtechnology.websocket;

import com.ovrtechnology.AromaAffect;
import net.blay09.mods.balm.client.platform.event.callback.ClientTickCallback;
import lombok.Getter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * <h2>Key Features:</h2>
 * <ul>
 * <li>Non-blocking connection that doesn't delay game startup</li>
 * <li>Automatic reconnection with exponential backoff</li>
 * <li>Health monitoring with ping/pong</li>
 * <li>Thread-safe message sending</li>
 * <li>Main thread callback execution</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
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

    // Custom device name reported by the bridge, or null if unknown
    @Getter
    private volatile String deviceName;

    private static final Pattern DEVICE_NAME_PATTERN =
            Pattern.compile("\"device_?name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.CASE_INSENSITIVE);

    // ========================================
    // Reconnection tracking
    // ========================================

    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicLong currentReconnectDelay = new AtomicLong(0);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private final AtomicBoolean everConnected = new AtomicBoolean(false);

    // Start of the current outage and connection attempts made during it, reported once
    // the connection is established. 0 means no outage is in progress.
    private final AtomicLong outageStartedAtMs = new AtomicLong(0);
    private final AtomicInteger outageAttempts = new AtomicInteger(0);
    // When the current connection was established, or 0 if not connected
    private final AtomicLong connectedAtMs = new AtomicLong(0);

    // ========================================
    // Diagnostic logging
    // ========================================

    private static final long REPEAT_LOG_INTERVAL_MS = 60_000;
    private final LogThrottle failureLog = new LogThrottle(REPEAT_LOG_INTERVAL_MS);
    private final LogThrottle retryLog = new LogThrottle(REPEAT_LOG_INTERVAL_MS);
    private final LogThrottle connectLog = new LogThrottle(REPEAT_LOG_INTERVAL_MS);
    private static final String CONNECTED_EVENT = "connected";
    private volatile ConnectionFailure lastFailureType = ConnectionFailure.UNKNOWN;

    // ========================================
    // Manual diagnostics (Bridge screen)
    // ========================================

    /**
     * The most recent connection problem.
     *
     * @param type        the failure category
     * @param detail      extra context such as {@code "HTTP 403"} or a close code, or null
     * @param timestampMs when the failure happened
     */
    public record FailureInfo(ConnectionFailure type, String detail, long timestampMs) {}

    /**
     * Outcome of {@link #testConnection()}.
     *
     * @param success     whether the bridge answered the test ping in time
     * @param roundTripMs the ping round trip, or -1 if no answer arrived
     * @param timestampMs when the test finished
     */
    public record TestResult(boolean success, long roundTripMs, long timestampMs) {}

    private static final long TEST_TIMEOUT_MS = 3_000;

    /** The most recent connection problem, or null if none has happened yet. */
    @Getter
    private volatile FailureInfo lastFailure;

    /** The result of the most recent connection test, or null if none has finished. */
    @Getter
    private volatile TestResult lastTestResult;

    // Send time of the pending test ping, or 0 if no test is running
    private final AtomicLong testPingSentAtMs = new AtomicLong(0);

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
    // Message history
    // ========================================

    private static final int MAX_MESSAGE_HISTORY = 50;
    private final Deque<WebSocketMessage> messageHistory = new ConcurrentLinkedDeque<>();

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
            Thread t = new Thread(r, "AromaAffect-WebSocket");
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
            AromaAffect.LOGGER.warn("OvrWebSocketClient.init() called multiple times!");
            return;
        }

        INSTANCE.config = config;

        AromaAffect.LOGGER.info("Initializing OVR WebSocket client...");
        AromaAffect.LOGGER.info("  Target: {}", config.getUri());
        AromaAffect.LOGGER.info("  Auto-connect: {}", config.isAutoConnect());
        AromaAffect.LOGGER.info("  Auto-reconnect: {}", config.isAutoReconnect());

        // Register tick handler for main thread execution
        ClientTickCallback.AFTER.register(instance -> {
            INSTANCE.processMainThreadQueue();
        });

        // Start connection if auto-connect is enabled
        if (config.isAutoConnect()) {
            INSTANCE.connectAsync();
        }

        AromaAffect.LOGGER.info("OVR WebSocket client initialized");
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
                AromaAffect.LOGGER.info("WebSocket URI changed, reconnecting...");
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
        // Compare-and-set so that the reconnect timer and the Bridge screen's button
        // cannot start two attempts at once
        ConnectionState currentState;
        do {
            currentState = state.get();
            if (currentState == ConnectionState.CONNECTING || currentState == ConnectionState.CONNECTED) {
                if (config.isDebugLogging()) {
                    AromaAffect.LOGGER.debug("Already {} - ignoring connect request", currentState);
                }
                return CompletableFuture.completedFuture(currentState == ConnectionState.CONNECTED);
            }
        } while (!state.compareAndSet(currentState, ConnectionState.CONNECTING));
        notifyStateChanged(currentState, ConnectionState.CONNECTING);
        markOutageStart();
        outageAttempts.incrementAndGet();
        // Captured so a slow attempt is reported against the URI it used, even if the
        // configuration changes while it is in flight
        String targetUri = config.getUri();

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (config.isDebugLogging()) {
                    AromaAffect.LOGGER.debug("Connecting to {}...", targetUri);
                }

                URI uri = URI.create(targetUri);

                // The builder's connect timeout bounds the whole opening handshake. The extra
                // second on get() lets its HttpTimeoutException, which says which phase timed
                // out, arrive before the generic TimeoutException from get().
                WebSocket ws = httpClient.newWebSocketBuilder()
                        .connectTimeout(Duration.ofMillis(config.getConnectionTimeoutMs()))
                        .buildAsync(uri, this)
                        .get(config.getConnectionTimeoutMs() + 1_000, TimeUnit.MILLISECONDS);

                this.webSocket = ws;
                setState(ConnectionState.CONNECTED);
                everConnected.set(true);

                // Reset reconnect counters on successful connection
                reconnectAttempts.set(0);
                currentReconnectDelay.set(config.getInitialReconnectDelayMs());
                long now = System.currentTimeMillis();
                long elapsedMs = now - outageStartedAtMs.getAndSet(0);
                int attemptsNeeded = outageAttempts.getAndSet(0);
                connectedAtMs.set(now);
                // A test result describes the previous connection, not this one
                testPingSentAtMs.set(0);
                lastTestResult = null;
                logConnected(targetUri, attemptsNeeded, elapsedMs);

                // Start health monitoring (can be disabled for debugging by setting interval to 0)
                if (config.getHealthCheckIntervalMs() > 0) {
                    startHealthMonitoring();
                } else {
                    AromaAffect.LOGGER.info("Health monitoring disabled (interval=0)");
                }

                // Notify listeners on main thread
                executeOnMainThread(() -> {
                    for (WebSocketConnectionListener listener : connectionListeners) {
                        try {
                            listener.onConnected();
                        } catch (Exception e) {
                            AromaAffect.LOGGER.error("Error in connection listener", e);
                        }
                    }
                });

                if (config.isDebugLogging()) {
                    AromaAffect.LOGGER.debug("OVR WebSocket ready to send/receive messages");
                }
                return true;

            } catch (Exception e) {
                handleConnectionFailure(e, targetUri);
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
            this.deviceName = null;
            // A deliberate disconnect ends the outage instead of extending it
            outageStartedAtMs.set(0);
            outageAttempts.set(0);
            connectedAtMs.set(0);
            failureLog.reset();
            retryLog.reset();
            connectLog.reset();
        }

        stopHealthMonitoring();

        WebSocket ws = this.webSocket;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, reason);
            } catch (Exception e) {
                if (config.isDebugLogging()) {
                    AromaAffect.LOGGER.debug("Error during disconnect: {}", ConnectionFailure.describeCauseChain(e), e);
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
                    AromaAffect.LOGGER.error("Error in connection listener", e);
                }
            }
        });

        AromaAffect.LOGGER.info("Disconnected from OVR WebSocket: {}", reason);
    }

    /**
     * Connects immediately, skipping any pending reconnection delay. Used by the
     * Bridge screen's Reconnect button. Failures of this attempt are logged in full
     * even if the same failure was logged recently.
     *
     * @return a future that completes with true if the connection succeeded; completes
     *         immediately if a connection is already established or in progress
     */
    public CompletableFuture<Boolean> reconnectNow() {
        ConnectionState current = state.get();
        if (current == ConnectionState.CONNECTING || current == ConnectionState.CONNECTED) {
            return CompletableFuture.completedFuture(current == ConnectionState.CONNECTED);
        }

        AromaAffect.LOGGER.info("Manual reconnect to OVR bridge at {} requested (state {})",
                config.getUri(), current);
        cancelReconnectTask();
        reconnectAttempts.set(0);
        currentReconnectDelay.set(config.getInitialReconnectDelayMs());
        failureLog.reset();
        retryLog.reset();
        return connectAsync();
    }

    /**
     * Sends a ping over the open connection and records the round trip in
     * {@code getLastTestResult()}. Does nothing if not connected or if a test is
     * already running.
     */
    public void testConnection() {
        WebSocket ws = this.webSocket;
        if (state.get() != ConnectionState.CONNECTED || ws == null) {
            return;
        }
        long sentAt = System.currentTimeMillis();
        if (!testPingSentAtMs.compareAndSet(0, sentAt)) {
            return;
        }

        AromaAffect.LOGGER.info("Testing OVR bridge connection at {}", config.getUri());
        try {
            ws.sendPing(ByteBuffer.allocate(0));
        } catch (Exception e) {
            testPingSentAtMs.set(0);
            lastTestResult = new TestResult(false, -1, System.currentTimeMillis());
            AromaAffect.LOGGER.warn("Connection test failed: could not send ping: {}",
                    ConnectionFailure.describeCauseChain(e), e);
            return;
        }

        scheduler.schedule(() -> {
            if (testPingSentAtMs.compareAndSet(sentAt, 0)) {
                lastTestResult = new TestResult(false, -1, System.currentTimeMillis());
                AromaAffect.LOGGER.warn("Connection test failed [{}]: no pong from OVR bridge within {} ms",
                        ConnectionFailure.TIMEOUT, TEST_TIMEOUT_MS);
            }
        }, TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Checks whether a connection test is waiting for an answer.
     *
     * @return true while a test ping is pending
     */
    public boolean isTestRunning() {
        return testPingSentAtMs.get() != 0;
    }

    /**
     * Shuts down the client completely.
     * Should be called when the mod is being unloaded.
     */
    public void shutdown() {
        AromaAffect.LOGGER.info("Shutting down OVR WebSocket client...");

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
                AromaAffect.LOGGER.debug("Cannot send message - not connected. State: {}", state.get());
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
            addToHistory(message);

            // Always log sent messages at info level for debugging OVR communication
            AromaAffect.LOGGER.info("WebSocket SENT: {}", rawText);
            if (config.isDebugLogging()) {
                AromaAffect.LOGGER.debug("Sent message object: {}", message);
            }

            return true;
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error sending message to OVR bridge: {}", ConnectionFailure.describeCauseChain(e), e);
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

    public boolean hasEverConnected() {
        return everConnected.get();
    }

    /**
     * Gets a snapshot of the recent message history (newest first).
     *
     * @return an immutable copy of the message history
     */
    public List<WebSocketMessage> getMessageHistory() {
        return List.copyOf(messageHistory);
    }

    private void addToHistory(WebSocketMessage message) {
        messageHistory.addFirst(message);
        while (messageHistory.size() > MAX_MESSAGE_HISTORY) {
            messageHistory.removeLast();
        }
    }

    // Bridge contract for the device name shown on the title screen. The bridge may report
    // the user's custom Omara name either as a "deviceName:<name>" message or inside any JSON
    // payload carrying a "deviceName"/"device_name" field (e.g. a "status" message).
    private void maybeCaptureDeviceName(WebSocketMessage message) {
        String type = message.getType();
        if (type != null) {
            String lowerType = type.toLowerCase(Locale.ROOT);
            if (lowerType.equals("devicename") || lowerType.equals("device_name")) {
                applyDeviceName(message.getPayload());
                return;
            }
        }

        Matcher matcher = DEVICE_NAME_PATTERN.matcher(message.getPayload());
        if (matcher.find()) {
            applyDeviceName(matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
        }
    }

    private void applyDeviceName(String name) {
        if (name == null) {
            return;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        this.deviceName = trimmed;
        if (config.isDebugLogging()) {
            AromaAffect.LOGGER.debug("Omara device name reported: {}", trimmed);
        }
    }

    // ========================================
    // WebSocket.Listener implementation
    // ========================================

    @Override
    public void onOpen(WebSocket webSocket) {
        // Track the new socket as soon as it opens so that close or error callbacks from
        // a previous socket can be told apart from this one
        this.webSocket = webSocket;
        if (config.isDebugLogging()) {
            AromaAffect.LOGGER.debug("WebSocket opened");
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
                AromaAffect.LOGGER.debug("Received: {}", message);
            }

            // Handle ping/pong internally
            if (message.isPing()) {
                send(WebSocketMessage.pong());
            } else if (message.isPong()) {
                lastPongTime.set(System.currentTimeMillis());
                awaitingPong.set(false);
            } else {
                maybeCaptureDeviceName(message);
                addToHistory(message);
                // Dispatch to handlers on main thread
                executeOnMainThread(() -> {
                    for (WebSocketMessageHandler handler : messageHandlers) {
                        try {
                            handler.onMessage(message);
                        } catch (Exception e) {
                            AromaAffect.LOGGER.error("Error in message handler", e);
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
            AromaAffect.LOGGER.debug("Received binary message ({} bytes)", data.remaining());
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        if (config.isDebugLogging()) {
            AromaAffect.LOGGER.debug("Received WebSocket ping");
        }
        // java.net.http.WebSocket typically auto-responds to ping frames, but sending
        // an explicit pong can improve compatibility with some servers.
        try {
            webSocket.sendPong(message);
        } catch (Exception e) {
            if (config.isDebugLogging()) {
                AromaAffect.LOGGER.debug("Failed to send WebSocket pong: {}", e.getMessage());
            }
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        long now = System.currentTimeMillis();
        lastPongTime.set(now);
        awaitingPong.set(false);
        // Any pong after the test ping answers the test; a health check pong arriving in
        // between only shortens the measured round trip
        long testSentAt = testPingSentAtMs.get();
        if (testSentAt != 0 && testPingSentAtMs.compareAndSet(testSentAt, 0)) {
            long roundTrip = now - testSentAt;
            lastTestResult = new TestResult(true, roundTrip, now);
            AromaAffect.LOGGER.info("Connection test OK: OVR bridge answered in {} ms", roundTrip);
        }
        if (config.isDebugLogging()) {
            AromaAffect.LOGGER.debug("Received WebSocket pong");
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (isStale(webSocket)) {
            if (config.isDebugLogging()) {
                AromaAffect.LOGGER.debug("Ignoring close of a previous OVR bridge connection: code {}",
                        ConnectionFailure.describeCloseCode(statusCode));
            }
            return null;
        }
        this.webSocket = null;
        stopHealthMonitoring();

        boolean wasClean = statusCode == WebSocket.NORMAL_CLOSURE;
        logClose(statusCode, reason, wasClean);

        executeOnMainThread(() -> {
            for (WebSocketConnectionListener listener : connectionListeners) {
                try {
                    listener.onDisconnected(reason, wasClean);
                } catch (Exception e) {
                    AromaAffect.LOGGER.error("Error in connection listener", e);
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
        if (isStale(webSocket)) {
            if (config.isDebugLogging()) {
                AromaAffect.LOGGER.debug("Ignoring error of a previous OVR bridge connection: {}",
                        ConnectionFailure.describeCauseChain(error));
            }
            return;
        }
        onConnectionLost();
        logConnectionFailure("Connection error", error, config.getUri());

        // Clean up current connection
        this.webSocket = null;
        stopHealthMonitoring();

        executeOnMainThread(() -> {
            for (WebSocketConnectionListener listener : connectionListeners) {
                try {
                    listener.onError(error);
                } catch (Exception e) {
                    AromaAffect.LOGGER.error("Error in connection listener", e);
                }
            }
        });

        // Schedule reconnection if enabled
        if (config.isAutoReconnect() && state.get() != ConnectionState.DISCONNECTED) {
            setState(ConnectionState.CONNECTION_FAILED);
            scheduleReconnect();
        } else {
            setState(ConnectionState.DISCONNECTED);
        }
    }

    // ========================================
    // Reconnection logic
    // ========================================

    private void handleConnectionFailure(Exception e, String targetUri) {
        setState(ConnectionState.CONNECTION_FAILED);
        logConnectionFailure("Connection attempt failed", e, targetUri);

        executeOnMainThread(() -> {
            for (WebSocketConnectionListener listener : connectionListeners) {
                try {
                    listener.onError(e);
                } catch (Exception ex) {
                    AromaAffect.LOGGER.error("Error in connection listener", ex);
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
            AromaAffect.LOGGER.warn("Max reconnection attempts ({}) reached. Giving up.", maxAttempts);
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

        logRetry(attempts, delay);

        setState(ConnectionState.RECONNECTING);

        // Notify listeners
        executeOnMainThread(() -> {
            for (WebSocketConnectionListener listener : connectionListeners) {
                try {
                    listener.onReconnecting(attempts, finalDelay);
                } catch (Exception e) {
                    AromaAffect.LOGGER.error("Error in connection listener", e);
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
    // Diagnostic logging
    // ========================================

    /**
     * A socket is stale once it has been replaced or disconnected by this client. Its close
     * and error callbacks can arrive late, after a newer connection is already up, and
     * must not tear that connection down or schedule a reconnect.
     */
    private boolean isStale(WebSocket socket) {
        return socket != this.webSocket;
    }

    private void markOutageStart() {
        outageStartedAtMs.compareAndSet(0, System.currentTimeMillis());
    }

    /**
     * Starts an outage when an established connection drops. Throttles are reset only if
     * the connection had been stable for a full log interval, so a bridge that keeps
     * dropping right after connecting stays rate limited while a fresh outage after a
     * stable session is logged in detail again.
     */
    private void onConnectionLost() {
        long now = System.currentTimeMillis();
        long connectedAt = connectedAtMs.getAndSet(0);
        if (connectedAt != 0 && now - connectedAt >= REPEAT_LOG_INTERVAL_MS) {
            failureLog.reset();
            retryLog.reset();
            connectLog.reset();
        }
        outageStartedAtMs.compareAndSet(0, now);
    }

    private long outageElapsedMs() {
        long start = outageStartedAtMs.get();
        return start == 0 ? 0 : System.currentTimeMillis() - start;
    }

    private static String formatSeconds(long ms) {
        return String.format(Locale.ROOT, "%.1f", ms / 1000.0);
    }

    /**
     * Logs a connection error with its category. The first error of a category is logged
     * with the full cause chain and stack trace; repeats are summarized once per
     * {@link #REPEAT_LOG_INTERVAL_MS}.
     */
    private void logConnectionFailure(String what, Throwable error, String targetUri) {
        ConnectionFailure type = ConnectionFailure.classify(error);
        lastFailureType = type;
        OptionalInt status = ConnectionFailure.handshakeStatus(error);
        lastFailure = new FailureInfo(type, status.isPresent() ? "HTTP " + status.getAsInt() : null,
                System.currentTimeMillis());
        String chain = ConnectionFailure.describeCauseChain(error);
        LogThrottle.Decision decision = failureLog.record(type);

        switch (decision.level()) {
            case DETAILED -> {
                String detail = status.isPresent() ? " (HTTP " + status.getAsInt() + ")" : "";
                String message = "{} [{}] to OVR bridge at {}{}: {}";
                // Without hardware a refused connection at startup is the normal case
                if (type == ConnectionFailure.REFUSED && !everConnected.get()) {
                    AromaAffect.LOGGER.info(message, what, type, targetUri, detail, chain, error);
                } else {
                    AromaAffect.LOGGER.warn(message, what, type, targetUri, detail, chain, error);
                }
            }
            case SUMMARY -> AromaAffect.LOGGER.info(
                    "Still unable to reach OVR bridge at {} [{}]: {} attempt(s) over {} s, {} repeated failure(s) not logged",
                    targetUri, type, outageAttempts.get(), formatSeconds(outageElapsedMs()),
                    decision.suppressed());
            case SUPPRESSED -> {
                if (config.isDebugLogging()) {
                    AromaAffect.LOGGER.debug("{} [{}]: {}", what, type, chain);
                }
            }
        }
    }

    private void logConnected(String targetUri, int attempts, long elapsedMs) {
        LogThrottle.Decision decision = connectLog.record(CONNECTED_EVENT);
        switch (decision.level()) {
            case DETAILED -> AromaAffect.LOGGER.info(
                    "=== OVR WebSocket CONNECTED to {} after {} attempt(s) / {} s ===",
                    targetUri, attempts, formatSeconds(elapsedMs));
            case SUMMARY -> AromaAffect.LOGGER.info(
                    "=== OVR WebSocket CONNECTED to {} after {} attempt(s) / {} s ({} earlier reconnection(s) not logged) ===",
                    targetUri, attempts, formatSeconds(elapsedMs), decision.suppressed());
            case SUPPRESSED -> {
                if (config.isDebugLogging()) {
                    AromaAffect.LOGGER.debug("OVR WebSocket connected to {} after {} attempt(s) / {} s",
                            targetUri, attempts, formatSeconds(elapsedMs));
                }
            }
        }
    }

    private void logClose(int statusCode, String reason, boolean wasClean) {
        String code = ConnectionFailure.describeCloseCode(statusCode);
        String shownReason = reason == null || reason.isEmpty() ? "none" : reason;
        if (wasClean) {
            AromaAffect.LOGGER.info("OVR bridge closed the connection: code {}, reason: {}", code, shownReason);
            return;
        }

        onConnectionLost();
        lastFailureType = ConnectionFailure.CLOSED;
        lastFailure = new FailureInfo(ConnectionFailure.CLOSED, code, System.currentTimeMillis());
        LogThrottle.Decision decision = failureLog.record(ConnectionFailure.CLOSED);
        switch (decision.level()) {
            case DETAILED -> AromaAffect.LOGGER.warn(
                    "OVR bridge connection closed unexpectedly [{}]: code {}, reason: {}",
                    ConnectionFailure.CLOSED, code, shownReason);
            case SUMMARY -> AromaAffect.LOGGER.warn(
                    "OVR bridge connection keeps closing [{}]: code {}, reason: {}, {} repeated close(s) not logged",
                    ConnectionFailure.CLOSED, code, shownReason, decision.suppressed());
            case SUPPRESSED -> {
                if (config.isDebugLogging()) {
                    AromaAffect.LOGGER.debug("OVR bridge connection closed: code {}, reason: {}", code, shownReason);
                }
            }
        }
    }

    private void logRetry(int retry, long delayMs) {
        LogThrottle.Decision decision = retryLog.record(lastFailureType);
        switch (decision.level()) {
            case DETAILED -> AromaAffect.LOGGER.info(
                    "Retrying OVR bridge connection in {} ms (retry {}, last failure {}); repeats are summarized every {} s",
                    delayMs, retry, lastFailureType, REPEAT_LOG_INTERVAL_MS / 1000);
            case SUMMARY -> AromaAffect.LOGGER.info(
                    "Retrying OVR bridge connection in {} ms (retry {}, last failure {}), {} retry message(s) not logged",
                    delayMs, retry, lastFailureType, decision.suppressed());
            case SUPPRESSED -> {
                if (config.isDebugLogging()) {
                    AromaAffect.LOGGER.debug("Retrying OVR bridge connection in {} ms (retry {})", delayMs, retry);
                }
            }
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
            AromaAffect.LOGGER.debug("Health monitoring started (interval: {} ms)", config.getHealthCheckIntervalMs());
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
                AromaAffect.LOGGER.warn("Health check failed [{}]: no pong received from OVR bridge in {} ms",
                        ConnectionFailure.TIMEOUT, elapsed);
                onConnectionLost();
                lastFailureType = ConnectionFailure.TIMEOUT;
                lastFailure = new FailureInfo(ConnectionFailure.TIMEOUT, null, System.currentTimeMillis());
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
            AromaAffect.LOGGER.warn("Health check ping failed: {}", ConnectionFailure.describeCauseChain(e), e);
        }

        if (config.isDebugLogging()) {
            AromaAffect.LOGGER.debug("Health check: ping sent");
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
                AromaAffect.LOGGER.error("Error executing main thread task", e);
            }
            processed++;
        }
    }

    // ========================================
    // State management
    // ========================================

    private void setState(ConnectionState newState) {
        ConnectionState oldState = state.getAndSet(newState);
        notifyStateChanged(oldState, newState);
    }

    private void notifyStateChanged(ConnectionState oldState, ConnectionState newState) {
        if (oldState != newState) {
            if (config.isDebugLogging()) {
                AromaAffect.LOGGER.debug("State changed: {} -> {}", oldState, newState);
            }

            executeOnMainThread(() -> {
                for (WebSocketConnectionListener listener : connectionListeners) {
                    try {
                        listener.onStateChanged(oldState, newState);
                    } catch (Exception e) {
                        AromaAffect.LOGGER.error("Error in connection listener", e);
                    }
                }
            });
        }
    }
}
