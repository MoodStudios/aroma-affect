package com.ovrtechnology.websocket;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link WebSocketMessage}. */
@DisplayName("WebSocketMessage")
class WebSocketMessageTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create message with type and payload")
        void shouldCreateWithTypeAndPayload() {
            WebSocketMessage msg = new WebSocketMessage("test", "data");

            assertThat(msg.getType()).isEqualTo("test");
            assertThat(msg.getPayload()).isEqualTo("data");
            assertThat(msg.isOutgoing()).isTrue();
            assertThat(msg.getId()).isNotEmpty();
            assertThat(msg.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("should create message with null payload as empty string")
        void shouldHandleNullPayload() {
            WebSocketMessage msg = new WebSocketMessage("test", null);

            assertThat(msg.getPayload()).isEmpty();
        }

        @Test
        @DisplayName("should throw on null type")
        void shouldThrowOnNullType() {
            assertThatThrownBy(() -> new WebSocketMessage(null, "data"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("type");
        }

        @Test
        @DisplayName("should create incoming message correctly")
        void shouldCreateIncomingMessage() {
            WebSocketMessage msg = new WebSocketMessage("response", "{}", false);

            assertThat(msg.isOutgoing()).isFalse();
        }

        @Test
        @DisplayName("should create message with all fields")
        void shouldCreateWithAllFields() {
            Instant now = Instant.now();
            WebSocketMessage msg = new WebSocketMessage("id123", "type", "payload", now, true);

            assertThat(msg.getId()).isEqualTo("id123");
            assertThat(msg.getType()).isEqualTo("type");
            assertThat(msg.getPayload()).isEqualTo("payload");
            assertThat(msg.getTimestamp()).isEqualTo(now);
            assertThat(msg.isOutgoing()).isTrue();
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("ping() should create ping message")
        void pingShouldWork() {
            WebSocketMessage msg = WebSocketMessage.ping();

            assertThat(msg.getType()).isEqualTo("ping");
            assertThat(msg.getPayload()).isEmpty();
            assertThat(msg.isPing()).isTrue();
            assertThat(msg.isPong()).isFalse();
        }

        @Test
        @DisplayName("pong() should create pong message")
        void pongShouldWork() {
            WebSocketMessage msg = WebSocketMessage.pong();

            assertThat(msg.getType()).isEqualTo("pong");
            assertThat(msg.getPayload()).isEmpty();
            assertThat(msg.isPong()).isTrue();
            assertThat(msg.isPing()).isFalse();
        }

        @Test
        @DisplayName("scent() should create scent message with intensity")
        void scentShouldWork() {
            WebSocketMessage msg = WebSocketMessage.scent("lavender", 0.75);

            assertThat(msg.getType()).isEqualTo("scent");
            assertThat(msg.getPayload()).contains("lavender");
            assertThat(msg.getPayload()).contains("0.75");
            assertThat(msg.isScent()).isTrue();
        }

        @Test
        @DisplayName("scent() should clamp intensity to [0, 1]")
        void scentShouldClampIntensity() {
            WebSocketMessage lowMsg = WebSocketMessage.scent("test", -0.5);
            WebSocketMessage highMsg = WebSocketMessage.scent("test", 1.5);

            assertThat(lowMsg.getPayload()).contains("0.00");
            assertThat(highMsg.getPayload()).contains("1.00");
        }

        @Test
        @DisplayName("stop() should create stop message")
        void stopShouldWork() {
            WebSocketMessage msg = WebSocketMessage.stop();

            assertThat(msg.getType()).isEqualTo("stop");
            assertThat(msg.getPayload()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Raw Text Serialization")
    class RawTextSerialization {

        @Test
        @DisplayName("toRawText should format type:payload")
        void toRawTextWithPayload() {
            WebSocketMessage msg = new WebSocketMessage("event", "{\"key\":\"value\"}");

            assertThat(msg.toRawText()).isEqualTo("event:{\"key\":\"value\"}");
        }

        @Test
        @DisplayName("toRawText should return just type when payload is empty")
        void toRawTextWithoutPayload() {
            WebSocketMessage msg = new WebSocketMessage("ping", "");

            assertThat(msg.toRawText()).isEqualTo("ping");
        }

        @Test
        @DisplayName("fromRawText should parse type:payload format")
        void fromRawTextWithPayload() {
            WebSocketMessage msg = WebSocketMessage.fromRawText("event:{\"data\":123}");

            assertThat(msg.getType()).isEqualTo("event");
            assertThat(msg.getPayload()).isEqualTo("{\"data\":123}");
            assertThat(msg.isOutgoing()).isFalse();
        }

        @Test
        @DisplayName("fromRawText should parse type-only format")
        void fromRawTextWithoutPayload() {
            WebSocketMessage msg = WebSocketMessage.fromRawText("ping");

            assertThat(msg.getType()).isEqualTo("ping");
            assertThat(msg.getPayload()).isEmpty();
        }

        @Test
        @DisplayName("fromRawText should handle empty input")
        void fromRawTextEmpty() {
            WebSocketMessage msg = WebSocketMessage.fromRawText("");

            assertThat(msg.getType()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("fromRawText should handle null input")
        void fromRawTextNull() {
            WebSocketMessage msg = WebSocketMessage.fromRawText(null);

            assertThat(msg.getType()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("fromRawText should handle payload with colons")
        void fromRawTextPayloadWithColons() {
            WebSocketMessage msg = WebSocketMessage.fromRawText("data:http://example.com:8080");

            assertThat(msg.getType()).isEqualTo("data");
            assertThat(msg.getPayload()).isEqualTo("http://example.com:8080");
        }
    }

    @Nested
    @DisplayName("Type Checks")
    class TypeChecks {

        @ParameterizedTest
        @ValueSource(strings = {"ping", "PING", "Ping", "pInG"})
        @DisplayName("isPing should be case-insensitive")
        void isPingShouldBeCaseInsensitive(String type) {
            WebSocketMessage msg = new WebSocketMessage(type, "");

            assertThat(msg.isPing()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"pong", "PONG", "Pong", "pOnG"})
        @DisplayName("isPong should be case-insensitive")
        void isPongShouldBeCaseInsensitive(String type) {
            WebSocketMessage msg = new WebSocketMessage(type, "");

            assertThat(msg.isPong()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"scent", "SCENT", "Scent", "sCeNt"})
        @DisplayName("isScent should be case-insensitive")
        void isScentShouldBeCaseInsensitive(String type) {
            WebSocketMessage msg = new WebSocketMessage(type, "");

            assertThat(msg.isScent()).isTrue();
        }
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityAndHashCode {

        @Test
        @DisplayName("messages with same id should be equal")
        void sameIdShouldBeEqual() {
            Instant now = Instant.now();
            WebSocketMessage msg1 = new WebSocketMessage("id1", "type1", "payload1", now, true);
            WebSocketMessage msg2 = new WebSocketMessage("id1", "type2", "payload2", now, false);

            assertThat(msg1).isEqualTo(msg2);
            assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
        }

        @Test
        @DisplayName("messages with different ids should not be equal")
        void differentIdShouldNotBeEqual() {
            WebSocketMessage msg1 = new WebSocketMessage("type", "payload");
            WebSocketMessage msg2 = new WebSocketMessage("type", "payload");

            // Each message has a unique UUID
            assertThat(msg1).isNotEqualTo(msg2);
        }

        @Test
        @DisplayName("message should equal itself")
        void shouldEqualItself() {
            WebSocketMessage msg = new WebSocketMessage("type", "payload");

            assertThat(msg).isEqualTo(msg);
        }

        @Test
        @DisplayName("message should not equal null")
        void shouldNotEqualNull() {
            WebSocketMessage msg = new WebSocketMessage("type", "payload");

            assertThat(msg).isNotEqualTo(null);
        }

        @Test
        @DisplayName("message should not equal different type")
        void shouldNotEqualDifferentType() {
            WebSocketMessage msg = new WebSocketMessage("type", "payload");

            assertThat(msg).isNotEqualTo("not a message");
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTests {

        @Test
        @DisplayName("should include type and id")
        void shouldIncludeBasicInfo() {
            WebSocketMessage msg = new WebSocketMessage("event", "data");
            String str = msg.toString();

            assertThat(str).contains("event");
            assertThat(str).contains(msg.getId());
        }

        @Test
        @DisplayName("should truncate long payloads")
        void shouldTruncateLongPayload() {
            String longPayload = "x".repeat(100);
            WebSocketMessage msg = new WebSocketMessage("event", longPayload);
            String str = msg.toString();

            assertThat(str).contains("...");
            assertThat(str.length()).isLessThan(200);
        }
    }
}
