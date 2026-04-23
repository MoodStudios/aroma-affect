package com.ovrtechnology.websocket;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.events.client.ClientTickEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;

public final class OvrWebSocketClient implements WebSocket.Listener {

    private static final OvrWebSocketClient INSTANCE = new OvrWebSocketClient();

    @Getter private volatile WebSocketConfig config = WebSocketConfig.DEFAULT;

    private final AtomicReference<ConnectionState> state =
            new AtomicReference<>(ConnectionState.DISCONNECTED);
    private volatile WebSocket webSocket;
    private final HttpClient httpClient;

    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicLong currentReconnectDelay = new AtomicLong(0);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    private final ScheduledExecutorService scheduler;
    private final Queue<Runnable> mainThreadQueue = new ConcurrentLinkedQueue<>();
    private ScheduledFuture<?> healthCheckTask;
    private ScheduledFuture<?> reconnectTask;

    private final List<WebSocketMessageHandler> messageHandlers = new CopyOnWriteArrayList<>();
    private final List<WebSocketConnectionListener> connectionListeners =
            new CopyOnWriteArrayList<>();
    private final StringBuilder messageBuffer = new StringBuilder();

    private static final int MAX_MESSAGE_HISTORY = 50;
    private final Deque<WebSocketMessage> messageHistory = new ConcurrentLinkedDeque<>();

    private final AtomicLong lastPongTime = new AtomicLong(0);
    private final AtomicBoolean awaitingPong = new AtomicBoolean(false);
    private final AtomicLong lastPingTime = new AtomicLong(0);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private OvrWebSocketClient() {
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofMillis(WebSocketConfig.DEFAULT.getConnectionTimeoutMs()))
                        .build();

        this.scheduler =
                Executors.newSingleThreadScheduledExecutor(
                        r -> {
                            Thread t = new Thread(r, "AromaAffect-WebSocket");
                            t.setDaemon(true);
                            return t;
                        });
    }

    public static OvrWebSocketClient getInstance() {
        return INSTANCE;
    }

    public static void init() {
        init(WebSocketConfig.DEFAULT);
    }

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

        ClientTickEvent.CLIENT_POST.register(
                instance -> {
                    INSTANCE.processMainThreadQueue();
                });

        if (config.isAutoConnect()) {
            INSTANCE.connectAsync();
        }

        AromaAffect.LOGGER.info("OVR WebSocket client initialized");
    }

    public CompletableFuture<Boolean> connectAsync() {
        ConnectionState currentState = state.get();
        if (currentState == ConnectionState.CONNECTING
                || currentState == ConnectionState.CONNECTED) {
            if (config.isDebugLogging()) {
                AromaAffect.LOGGER.debug("Already {} - ignoring connect request", currentState);
            }
            return CompletableFuture.completedFuture(currentState == ConnectionState.CONNECTED);
        }

        setState(ConnectionState.CONNECTING);

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        if (config.isDebugLogging()) {
                            AromaAffect.LOGGER.debug("Connecting to {}...", config.getUri());
                        }

                        URI uri = URI.create(config.getUri());

                        WebSocket ws =
                                httpClient
                                        .newWebSocketBuilder()
                                        .connectTimeout(
                                                Duration.ofMillis(config.getConnectionTimeoutMs()))
                                        .buildAsync(uri, this)
                                        .get(
                                                config.getConnectionTimeoutMs(),
                                                TimeUnit.MILLISECONDS);

                        this.webSocket = ws;
                        setState(ConnectionState.CONNECTED);

                        reconnectAttempts.set(0);
                        currentReconnectDelay.set(config.getInitialReconnectDelayMs());

                        AromaAffect.LOGGER.info(
                                "=== OVR WebSocket CONNECTED to {} ===", config.getUri());

                        if (config.getHealthCheckIntervalMs() > 0) {
                            startHealthMonitoring();
                        } else {
                            AromaAffect.LOGGER.info("Health monitoring disabled (interval=0)");
                        }

                        executeOnMainThread(
                                () -> {
                                    for (WebSocketConnectionListener listener :
                                            connectionListeners) {
                                        try {
                                            listener.onConnected();
                                        } catch (Exception e) {
                                            AromaAffect.LOGGER.error(
                                                    "Error in connection listener", e);
                                        }
                                    }
                                });

                        AromaAffect.LOGGER.info("OVR WebSocket ready to send/receive messages");
                        return true;

                    } catch (Exception e) {
                        handleConnectionFailure(e);
                        return false;
                    }
                },
                scheduler);
    }

    public void disconnect() {
        disconnect("Manual disconnect", true);
    }

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
                    AromaAffect.LOGGER.debug("Error during disconnect: {}", e.getMessage());
                }
            }
            this.webSocket = null;
        }

        setState(ConnectionState.DISCONNECTED);

        final String finalReason = reason;
        executeOnMainThread(
                () -> {
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

    public boolean send(WebSocketMessage message) {
        if (!state.get().canSendMessages()) {
            if (config.isDebugLogging()) {
                AromaAffect.LOGGER.debug(
                        "Cannot send message - not connected. State: {}", state.get());
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

            AromaAffect.LOGGER.info("WebSocket SENT: {}", rawText);
            if (config.isDebugLogging()) {
                AromaAffect.LOGGER.debug("Sent message object: {}", message);
            }

            return true;
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error sending message: {}", e.getMessage());
            return false;
        }
    }

    public void addConnectionListener(WebSocketConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public ConnectionState getState() {
        return state.get();
    }

    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }

    public List<WebSocketMessage> getMessageHistory() {
        return List.copyOf(messageHistory);
    }

    private void addToHistory(WebSocketMessage message) {
        messageHistory.addFirst(message);
        while (messageHistory.size() > MAX_MESSAGE_HISTORY) {
            messageHistory.removeLast();
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
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

            if (message.isPing()) {
                send(WebSocketMessage.pong());
            } else if (message.isPong()) {
                lastPongTime.set(System.currentTimeMillis());
                awaitingPong.set(false);
            } else {
                addToHistory(message);

                executeOnMainThread(
                        () -> {
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
        lastPongTime.set(System.currentTimeMillis());
        awaitingPong.set(false);
        if (config.isDebugLogging()) {
            AromaAffect.LOGGER.debug("Received WebSocket pong");
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        AromaAffect.LOGGER.info("WebSocket closed: {} (code: {})", reason, statusCode);

        this.webSocket = null;
        stopHealthMonitoring();

        boolean wasClean = statusCode == WebSocket.NORMAL_CLOSURE;

        executeOnMainThread(
                () -> {
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
        AromaAffect.LOGGER.error("WebSocket error: {}", error.getMessage());

        this.webSocket = null;
        stopHealthMonitoring();

        executeOnMainThread(
                () -> {
                    for (WebSocketConnectionListener listener : connectionListeners) {
                        try {
                            listener.onError(error);
                        } catch (Exception e) {
                            AromaAffect.LOGGER.error("Error in connection listener", e);
                        }
                    }
                });

        if (config.isAutoReconnect() && state.get() != ConnectionState.DISCONNECTED) {
            setState(ConnectionState.CONNECTION_FAILED);
            scheduleReconnect();
        } else {
            setState(ConnectionState.DISCONNECTED);
        }
    }

    private void handleConnectionFailure(Exception e) {
        setState(ConnectionState.CONNECTION_FAILED);

        if (reconnectAttempts.get() == 0) {
            String errorMsg = e.getMessage();
            if (e.getCause() != null) {
                errorMsg = e.getCause().getMessage();
            }
            AromaAffect.LOGGER.info(
                    "OVR WebSocket connection failed ({}): {}", config.getUri(), errorMsg);
            AromaAffect.LOGGER.info("Will keep trying to reconnect in background...");
        }

        executeOnMainThread(
                () -> {
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
            return;
        }

        int attempts = reconnectAttempts.incrementAndGet();
        int maxAttempts = config.getMaxReconnectAttempts();

        if (maxAttempts > 0 && attempts > maxAttempts) {
            AromaAffect.LOGGER.warn(
                    "Max reconnection attempts ({}) reached. Giving up.", maxAttempts);
            setState(ConnectionState.DISCONNECTED);
            reconnectScheduled.set(false);
            return;
        }

        long delay = currentReconnectDelay.get();
        if (delay == 0) {
            delay = config.getInitialReconnectDelayMs();
        }

        double jitter = 0.9 + (Math.random() * 0.2);
        delay = (long) (delay * jitter);

        delay = Math.min(delay, config.getMaxReconnectDelayMs());

        final long finalDelay = delay;

        if (config.isDebugLogging()) {
            AromaAffect.LOGGER.debug("Reconnecting in {} ms (attempt {})...", delay, attempts);
        }

        setState(ConnectionState.RECONNECTING);

        executeOnMainThread(
                () -> {
                    for (WebSocketConnectionListener listener : connectionListeners) {
                        try {
                            listener.onReconnecting(attempts, finalDelay);
                        } catch (Exception e) {
                            AromaAffect.LOGGER.error("Error in connection listener", e);
                        }
                    }
                });

        reconnectTask =
                scheduler.schedule(
                        () -> {
                            reconnectScheduled.set(false);

                            long nextDelay =
                                    (long)
                                            (currentReconnectDelay.get()
                                                    * config.getReconnectBackoffMultiplier());
                            currentReconnectDelay.set(
                                    Math.min(nextDelay, config.getMaxReconnectDelayMs()));

                            connectAsync();
                        },
                        delay,
                        TimeUnit.MILLISECONDS);
    }

    private void cancelReconnectTask() {
        reconnectScheduled.set(false);
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
    }

    private void startHealthMonitoring() {
        stopHealthMonitoring();

        long now = System.currentTimeMillis();
        lastPongTime.set(now);
        lastPingTime.set(now);
        awaitingPong.set(false);

        healthCheckTask =
                scheduler.scheduleAtFixedRate(
                        this::performHealthCheck,
                        config.getHealthCheckIntervalMs(),
                        config.getHealthCheckIntervalMs(),
                        TimeUnit.MILLISECONDS);

        if (config.isDebugLogging()) {
            AromaAffect.LOGGER.debug(
                    "Health monitoring started (interval: {} ms)",
                    config.getHealthCheckIntervalMs());
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

        if (awaitingPong.get()) {
            long elapsed = System.currentTimeMillis() - lastPingTime.get();
            if (elapsed > config.getHealthCheckIntervalMs() * 2) {
                AromaAffect.LOGGER.warn("Health check failed: No pong received in {} ms", elapsed);
                disconnect("Health check timeout", false);
                if (config.isAutoReconnect()) {
                    scheduleReconnect();
                }
                return;
            }
        }

        WebSocket ws = this.webSocket;
        if (ws == null) {
            return;
        }

        awaitingPong.set(true);
        lastPingTime.set(System.currentTimeMillis());
        try {
            ws.sendPing(ByteBuffer.allocate(0));
        } catch (Exception e) {
            AromaAffect.LOGGER.warn("Health check ping failed: {}", e.getMessage());
        }

        if (config.isDebugLogging()) {
            AromaAffect.LOGGER.debug("Health check: ping sent");
        }
    }

    public void executeOnMainThread(Runnable task) {
        mainThreadQueue.offer(task);
    }

    private void processMainThreadQueue() {

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

    private void setState(ConnectionState newState) {
        ConnectionState oldState = state.getAndSet(newState);

        if (oldState != newState) {
            if (config.isDebugLogging()) {
                AromaAffect.LOGGER.debug("State changed: {} -> {}", oldState, newState);
            }

            executeOnMainThread(
                    () -> {
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
