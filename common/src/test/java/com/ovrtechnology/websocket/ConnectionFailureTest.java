package com.ovrtechnology.websocket;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocketHandshakeException;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ConnectionFailure}.
 */
@DisplayName("ConnectionFailure")
class ConnectionFailureTest {

    @Nested
    @DisplayName("classify")
    class Classify {

        @Test
        @DisplayName("connection refused wrapped in ExecutionException is REFUSED")
        void refused() {
            Throwable error = new ExecutionException(new ConnectException("Connection refused"));
            assertThat(ConnectionFailure.classify(error)).isEqualTo(ConnectionFailure.REFUSED);
        }

        @Test
        @DisplayName("refusal as raised by the JDK HTTP client is REFUSED, not CLOSED")
        void refusedAsRaisedByHttpClient() {
            // The JDK client reports a refused connection as ConnectException(null)
            // caused by ClosedChannelException.
            ConnectException refused = new ConnectException();
            refused.initCause(new ClosedChannelException());
            Throwable error = new ExecutionException(refused);
            assertThat(ConnectionFailure.classify(error)).isEqualTo(ConnectionFailure.REFUSED);
        }

        @Test
        @DisplayName("HttpConnectTimeoutException with a ConnectException cause is TIMEOUT")
        void httpConnectTimeoutWinsOverItsCause() {
            HttpConnectTimeoutException timeout = new HttpConnectTimeoutException("HTTP connect timed out");
            timeout.initCause(new ConnectException("HTTP connect timed out"));
            Throwable error = new ExecutionException(timeout);
            assertThat(ConnectionFailure.classify(error)).isEqualTo(ConnectionFailure.TIMEOUT);
        }

        @Test
        @DisplayName("TimeoutException from Future.get is TIMEOUT")
        void futureTimeout() {
            assertThat(ConnectionFailure.classify(new TimeoutException()))
                    .isEqualTo(ConnectionFailure.TIMEOUT);
        }

        @Test
        @DisplayName("ConnectException reporting a timeout is TIMEOUT")
        void connectExceptionTimedOut() {
            Throwable error = new ConnectException("Connection timed out: getsockopt");
            assertThat(ConnectionFailure.classify(error)).isEqualTo(ConnectionFailure.TIMEOUT);
        }

        @Test
        @DisplayName("WebSocketHandshakeException is HANDSHAKE_REJECTED")
        void handshakeRejected() {
            Throwable error = new CompletionException(new WebSocketHandshakeException(response(403)));
            assertThat(ConnectionFailure.classify(error))
                    .isEqualTo(ConnectionFailure.HANDSHAKE_REJECTED);
        }

        @Test
        @DisplayName("closed channel, EOF and connection reset are CLOSED")
        void closed() {
            assertThat(ConnectionFailure.classify(new IOException(new ClosedChannelException())))
                    .isEqualTo(ConnectionFailure.CLOSED);
            assertThat(ConnectionFailure.classify(new EOFException()))
                    .isEqualTo(ConnectionFailure.CLOSED);
            assertThat(ConnectionFailure.classify(new SocketException("Connection reset")))
                    .isEqualTo(ConnectionFailure.CLOSED);
        }

        @Test
        @DisplayName("unrecognised errors and null are UNKNOWN")
        void unknown() {
            assertThat(ConnectionFailure.classify(new IllegalStateException("boom")))
                    .isEqualTo(ConnectionFailure.UNKNOWN);
            assertThat(ConnectionFailure.classify(new SocketException("Network is unreachable")))
                    .isEqualTo(ConnectionFailure.UNKNOWN);
            assertThat(ConnectionFailure.classify(null)).isEqualTo(ConnectionFailure.UNKNOWN);
        }

        @Test
        @DisplayName("a cyclic cause chain terminates")
        void cyclicChain() {
            Exception a = new Exception("a");
            Exception b = new Exception("b", a);
            a.initCause(b);
            assertThat(ConnectionFailure.classify(a)).isEqualTo(ConnectionFailure.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("describeCauseChain")
    class DescribeCauseChain {

        @Test
        @DisplayName("lists every cause with its fully qualified class name")
        void fullChain() {
            Throwable error = new ExecutionException("outer", new ConnectException("Connection refused"));
            assertThat(ConnectionFailure.describeCauseChain(error)).isEqualTo(
                    "java.util.concurrent.ExecutionException: outer"
                            + " <- java.net.ConnectException: Connection refused");
        }

        @Test
        @DisplayName("omits the message when it is null")
        void nullMessage() {
            assertThat(ConnectionFailure.describeCauseChain(new TimeoutException()))
                    .isEqualTo("java.util.concurrent.TimeoutException");
        }

        @Test
        @DisplayName("returns 'none' for null")
        void nullError() {
            assertThat(ConnectionFailure.describeCauseChain(null)).isEqualTo("none");
        }

        @Test
        @DisplayName("a cyclic cause chain terminates")
        void cyclicChain() {
            Exception a = new Exception("a");
            Exception b = new Exception("b", a);
            a.initCause(b);
            assertThat(ConnectionFailure.describeCauseChain(a))
                    .isEqualTo("java.lang.Exception: a <- java.lang.Exception: b");
        }
    }

    @Test
    @DisplayName("handshakeStatus returns the server's status code")
    void handshakeStatus() {
        Throwable error = new ExecutionException(new WebSocketHandshakeException(response(403)));
        assertThat(ConnectionFailure.handshakeStatus(error)).hasValue(403);
        assertThat(ConnectionFailure.handshakeStatus(new ConnectException())).isEmpty();
    }

    @Test
    @DisplayName("describeCloseCode names standard and custom ranges")
    void describeCloseCode() {
        assertThat(ConnectionFailure.describeCloseCode(1000)).isEqualTo("1000 (normal closure)");
        assertThat(ConnectionFailure.describeCloseCode(1006)).isEqualTo("1006 (abnormal closure)");
        assertThat(ConnectionFailure.describeCloseCode(3001)).isEqualTo("3001 (registered code)");
        assertThat(ConnectionFailure.describeCloseCode(4002)).isEqualTo("4002 (application-defined code)");
        assertThat(ConnectionFailure.describeCloseCode(42)).isEqualTo("42 (unknown code)");
    }

    private static HttpResponse<String> response(int statusCode) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(Map.of(), (name, value) -> true);
            }

            @Override
            public String body() {
                return null;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return URI.create("ws://127.0.0.1:8080");
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }
}
