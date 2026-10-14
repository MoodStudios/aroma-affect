package com.ovrtechnology.websocket;

import java.io.EOFException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.WebSocketHandshakeException;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Category of a failed or lost bridge connection.
 *
 * <p>{@link #classify(Throwable)} walks the cause chain from the outermost exception
 * inwards and returns the first recognized category, so wrappers such as
 * {@link java.util.concurrent.ExecutionException} are looked through. The outermost
 * match wins because the JDK HTTP client reports a connect timeout as an
 * {@link java.net.http.HttpConnectTimeoutException} whose cause is a
 * {@link ConnectException}.</p>
 */
public enum ConnectionFailure {

    /** Nothing is listening on the bridge port. */
    REFUSED("Connection refused"),

    /** The connection or opening handshake did not complete in time. */
    TIMEOUT("Connection timed out"),

    /** The server answered the HTTP upgrade request without switching protocols. */
    HANDSHAKE_REJECTED("WebSocket handshake rejected"),

    /** An established connection was closed or dropped. */
    CLOSED("Connection closed"),

    /** Any failure that does not match the categories above. */
    UNKNOWN("Unexpected error");

    private static final int MAX_CHAIN_DEPTH = 16;
    private static final String CHAIN_SEPARATOR = " <- ";

    private final String displayName;

    ConnectionFailure(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets a short human-readable name for this category.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Classifies a connection error.
     *
     * @param error the error, possibly wrapped; may be null
     * @return the category of the first recognized exception in the cause chain,
     *         or {@link #UNKNOWN}
     */
    public static ConnectionFailure classify(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = error;
        while (current != null && seen.size() < MAX_CHAIN_DEPTH && seen.add(current)) {
            ConnectionFailure type = classifySingle(current);
            if (type != UNKNOWN) {
                return type;
            }
            current = current.getCause();
        }
        return UNKNOWN;
    }

    private static ConnectionFailure classifySingle(Throwable t) {
        if (t instanceof WebSocketHandshakeException) {
            return HANDSHAKE_REJECTED;
        }
        if (t instanceof HttpTimeoutException
                || t instanceof TimeoutException
                || t instanceof SocketTimeoutException) {
            return TIMEOUT;
        }
        if (t instanceof ConnectException) {
            // Windows reports an unanswered SYN as "Connection timed out" through ConnectException.
            return messageContains(t, "timed out") ? TIMEOUT : REFUSED;
        }
        if (t instanceof ClosedChannelException || t instanceof EOFException) {
            return CLOSED;
        }
        if (t instanceof SocketException
                && (messageContains(t, "reset") || messageContains(t, "aborted"))) {
            return CLOSED;
        }
        return UNKNOWN;
    }

    private static boolean messageContains(Throwable t, String fragment) {
        String message = t.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(fragment);
    }

    /**
     * Formats the cause chain on one line, outermost first, using fully qualified
     * class names, e.g.
     * {@code java.util.concurrent.ExecutionException: ... <- java.net.ConnectException: Connection refused}.
     *
     * @param error the error; may be null
     * @return the formatted chain, or {@code "none"} when error is null
     */
    public static String describeCauseChain(Throwable error) {
        if (error == null) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = error;
        while (current != null && seen.add(current)) {
            if (seen.size() > MAX_CHAIN_DEPTH) {
                sb.append(CHAIN_SEPARATOR).append("...");
                break;
            }
            if (sb.length() > 0) {
                sb.append(CHAIN_SEPARATOR);
            }
            sb.append(current.getClass().getName());
            if (current.getMessage() != null) {
                sb.append(": ").append(current.getMessage());
            }
            current = current.getCause();
        }
        return sb.toString();
    }

    /**
     * Finds the HTTP status code of a rejected WebSocket handshake.
     *
     * @param error the error, possibly wrapped; may be null
     * @return the status code returned by the server, or empty if the chain holds no
     *         {@link WebSocketHandshakeException} with a response
     */
    public static OptionalInt handshakeStatus(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = error;
        while (current != null && seen.size() < MAX_CHAIN_DEPTH && seen.add(current)) {
            if (current instanceof WebSocketHandshakeException handshake) {
                HttpResponse<?> response = handshake.getResponse();
                return response != null ? OptionalInt.of(response.statusCode()) : OptionalInt.empty();
            }
            current = current.getCause();
        }
        return OptionalInt.empty();
    }

    /**
     * Describes a WebSocket close status code as defined by RFC 6455 section 7.4.
     *
     * @param statusCode the close status code
     * @return the code followed by its meaning, e.g. {@code "1006 (abnormal closure)"}
     */
    public static String describeCloseCode(int statusCode) {
        String meaning = switch (statusCode) {
            case 1000 -> "normal closure";
            case 1001 -> "going away";
            case 1002 -> "protocol error";
            case 1003 -> "unsupported data";
            case 1005 -> "no status received";
            case 1006 -> "abnormal closure";
            case 1007 -> "invalid payload data";
            case 1008 -> "policy violation";
            case 1009 -> "message too big";
            case 1010 -> "mandatory extension missing";
            case 1011 -> "internal server error";
            case 1012 -> "service restart";
            case 1013 -> "try again later";
            case 1014 -> "bad gateway";
            case 1015 -> "TLS handshake failure";
            default -> {
                if (statusCode >= 3000 && statusCode <= 3999) {
                    yield "registered code";
                }
                if (statusCode >= 4000 && statusCode <= 4999) {
                    yield "application-defined code";
                }
                yield "unknown code";
            }
        };
        return statusCode + " (" + meaning + ")";
    }
}
