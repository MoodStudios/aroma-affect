package com.ovrtechnology.websocket;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a WebSocket message for communication with OVR hardware.
 * 
 * <p>Messages have a type identifier and a payload. The type helps route
 * messages to appropriate handlers, while the payload contains the actual data.</p>
 * 
 * <h3>Common Message Types:</h3>
 * <ul>
 *   <li><b>scent</b> - Trigger a scent emission</li>
 *   <li><b>stop</b> - Stop current scent emission</li>
 *   <li><b>ping</b> - Health check ping</li>
 *   <li><b>pong</b> - Health check response</li>
 *   <li><b>config</b> - Configuration updates</li>
 *   <li><b>status</b> - Status updates from hardware</li>
 * </ul>
 */
@Getter
public class WebSocketMessage {
    
    /**
     * Unique identifier for this message.
     */
    private final String id;
    
    /**
     * Message type identifier.
     */
    private final String type;
    
    /**
     * Message payload (typically JSON).
     */
    private final String payload;
    
    /**
     * Timestamp when this message was created.
     */
    private final Instant timestamp;
    
    /**
     * Whether this is an outgoing (true) or incoming (false) message.
     */
    private final boolean outgoing;
    
    /**
     * Creates a new outgoing message.
     * 
     * @param type the message type
     * @param payload the message payload
     */
    public WebSocketMessage(String type, String payload) {
        this(UUID.randomUUID().toString(), type, payload, Instant.now(), true);
    }
    
    /**
     * Creates a new message with explicit direction.
     * 
     * @param type the message type
     * @param payload the message payload
     * @param outgoing true if outgoing, false if incoming
     */
    public WebSocketMessage(String type, String payload, boolean outgoing) {
        this(UUID.randomUUID().toString(), type, payload, Instant.now(), outgoing);
    }
    
    /**
     * Creates a message with all fields specified.
     * 
     * @param id unique message identifier
     * @param type the message type
     * @param payload the message payload
     * @param timestamp when the message was created
     * @param outgoing true if outgoing, false if incoming
     */
    public WebSocketMessage(String id, String type, String payload, Instant timestamp, boolean outgoing) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.payload = payload != null ? payload : "";
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.outgoing = outgoing;
    }
    
    /**
     * Creates a ping message.
     * 
     * @return a new ping message
     */
    public static WebSocketMessage ping() {
        return new WebSocketMessage("ping", "");
    }
    
    /**
     * Creates a pong message (response to ping).
     * 
     * @return a new pong message
     */
    public static WebSocketMessage pong() {
        return new WebSocketMessage("pong", "");
    }
    
    /**
     * Creates a scent trigger message.
     * 
     * @param scentId the scent identifier
     * @param intensity the intensity (0.0 to 1.0)
     * @return a new scent message
     */
    public static WebSocketMessage scent(String scentId, double intensity) {
        // Simple JSON format - in production, use a proper JSON library
        String payload = String.format("{\"scentId\":\"%s\",\"intensity\":%.2f}", 
                scentId, Math.max(0.0, Math.min(1.0, intensity)));
        return new WebSocketMessage("scent", payload);
    }
    
    /**
     * Creates a stop message to halt scent emission.
     * 
     * @return a new stop message
     */
    public static WebSocketMessage stop() {
        return new WebSocketMessage("stop", "");
    }
    
    /**
     * Parses a raw text message received from WebSocket.
     * 
     * <p>Expected format: type:payload or just type if no payload.</p>
     * 
     * @param rawText the raw text received
     * @return a parsed incoming message
     */
    public static WebSocketMessage fromRawText(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return new WebSocketMessage("unknown", "", false);
        }
        
        int colonIndex = rawText.indexOf(':');
        if (colonIndex > 0) {
            String type = rawText.substring(0, colonIndex).trim();
            String payload = rawText.substring(colonIndex + 1).trim();
            return new WebSocketMessage(type, payload, false);
        }
        
        return new WebSocketMessage(rawText.trim(), "", false);
    }
    
    /**
     * Converts this message to raw text for sending.
     * 
     * <p>Format: type:payload (or just type if payload is empty)</p>
     * 
     * @return the raw text representation
     */
    public String toRawText() {
        if (payload == null || payload.isEmpty()) {
            return type;
        }
        return type + ":" + payload;
    }
    
    /**
     * Checks if this is a ping message.
     * 
     * @return true if this is a ping
     */
    public boolean isPing() {
        return "ping".equalsIgnoreCase(type);
    }
    
    /**
     * Checks if this is a pong message.
     * 
     * @return true if this is a pong
     */
    public boolean isPong() {
        return "pong".equalsIgnoreCase(type);
    }
    
    /**
     * Checks if this is a scent message.
     * 
     * @return true if this is a scent trigger
     */
    public boolean isScent() {
        return "scent".equalsIgnoreCase(type);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketMessage that = (WebSocketMessage) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "WebSocketMessage{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", payload='" + (payload.length() > 50 ? payload.substring(0, 50) + "..." : payload) + '\'' +
                ", outgoing=" + outgoing +
                '}';
    }
}

