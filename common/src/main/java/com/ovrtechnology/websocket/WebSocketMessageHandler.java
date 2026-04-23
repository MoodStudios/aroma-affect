package com.ovrtechnology.websocket;

@FunctionalInterface
public interface WebSocketMessageHandler {

    void onMessage(WebSocketMessage message);
}
