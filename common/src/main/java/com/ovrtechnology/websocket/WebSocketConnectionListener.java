package com.ovrtechnology.websocket;

public interface WebSocketConnectionListener {

    default void onConnected() {}

    default void onStateChanged(ConnectionState oldState, ConnectionState newState) {}

    default void onDisconnected(String reason, boolean wasClean) {}

    default void onError(Throwable error) {}

    default void onReconnecting(int attemptNumber, long delayMs) {}
}
