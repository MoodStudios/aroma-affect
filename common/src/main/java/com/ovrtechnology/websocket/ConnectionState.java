package com.ovrtechnology.websocket;

import lombok.Getter;

public enum ConnectionState {
    DISCONNECTED("Disconnected"),

    CONNECTING("Connecting..."),

    CONNECTED("Connected"),

    CONNECTION_FAILED("Connection Failed"),

    RECONNECTING("Reconnecting...");

    @Getter private final String displayName;

    ConnectionState(String displayName) {
        this.displayName = displayName;
    }

    public boolean isActiveOrPending() {
        return this == CONNECTING || this == CONNECTED || this == RECONNECTING;
    }

    public boolean canSendMessages() {
        return this == CONNECTED;
    }
}
