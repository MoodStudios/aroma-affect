package com.ovrtechnology.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link WebSocketConfig}.
 */
@DisplayName("WebSocketConfig")
class WebSocketConfigTest {
    
    @Nested
    @DisplayName("Default Configuration")
    class DefaultConfiguration {
        
        @Test
        @DisplayName("should have sensible default values")
        void shouldHaveSensibleDefaults() {
            WebSocketConfig config = WebSocketConfig.DEFAULT;
            
            assertThat(config.getHost()).isEqualTo("localhost");
            assertThat(config.getPort()).isEqualTo(8080);
            assertThat(config.isSecure()).isFalse();
            assertThat(config.getPath()).isEmpty();
            assertThat(config.isAutoConnect()).isTrue();
            assertThat(config.isAutoReconnect()).isTrue();
            assertThat(config.getInitialReconnectDelayMs()).isEqualTo(1000L);
            assertThat(config.getMaxReconnectDelayMs()).isEqualTo(30000L);
            assertThat(config.getReconnectBackoffMultiplier()).isEqualTo(2.0);
            assertThat(config.getMaxReconnectAttempts()).isEqualTo(-1);
            assertThat(config.getHealthCheckIntervalMs()).isEqualTo(5000L);
            assertThat(config.getConnectionTimeoutMs()).isEqualTo(10000L);
        }
        
        @Test
        @DisplayName("should generate correct default URI")
        void shouldGenerateCorrectDefaultUri() {
            assertThat(WebSocketConfig.DEFAULT.getUri()).isEqualTo("ws://localhost:8080");
        }
    }
    
    @Nested
    @DisplayName("URI Generation")
    class UriGeneration {
        
        @Test
        @DisplayName("should build ws:// URI for non-secure connection")
        void shouldBuildWsUri() {
            WebSocketConfig config = WebSocketConfig.builder()
                    .host("example.com")
                    .port(9000)
                    .secure(false)
                    .build();
            
            assertThat(config.getUri()).isEqualTo("ws://example.com:9000");
        }
        
        @Test
        @DisplayName("should build wss:// URI for secure connection")
        void shouldBuildWssUri() {
            WebSocketConfig config = WebSocketConfig.builder()
                    .host("secure.example.com")
                    .port(443)
                    .secure(true)
                    .build();
            
            assertThat(config.getUri()).isEqualTo("wss://secure.example.com:443");
        }
        
        @Test
        @DisplayName("should include path in URI when specified")
        void shouldIncludePathInUri() {
            WebSocketConfig config = WebSocketConfig.builder()
                    .host("localhost")
                    .port(8080)
                    .path("/api/ws")
                    .build();
            
            assertThat(config.getUri()).isEqualTo("ws://localhost:8080/api/ws");
        }
        
        @Test
        @DisplayName("should add leading slash to path if missing")
        void shouldAddLeadingSlashToPath() {
            WebSocketConfig config = WebSocketConfig.builder()
                    .host("localhost")
                    .port(8080)
                    .path("socket")
                    .build();
            
            assertThat(config.getUri()).isEqualTo("ws://localhost:8080/socket");
        }
    }
    
    @Nested
    @DisplayName("URI Parsing")
    class UriParsing {
        
        @Test
        @DisplayName("should parse ws:// URI correctly")
        void shouldParseWsUri() {
            WebSocketConfig config = WebSocketConfig.fromUri("ws://myhost:3000/path");
            
            assertThat(config.getHost()).isEqualTo("myhost");
            assertThat(config.getPort()).isEqualTo(3000);
            assertThat(config.isSecure()).isFalse();
            assertThat(config.getPath()).isEqualTo("/path");
        }
        
        @Test
        @DisplayName("should parse wss:// URI correctly")
        void shouldParseWssUri() {
            WebSocketConfig config = WebSocketConfig.fromUri("wss://secure.host:443");
            
            assertThat(config.getHost()).isEqualTo("secure.host");
            assertThat(config.getPort()).isEqualTo(443);
            assertThat(config.isSecure()).isTrue();
        }
        
        @Test
        @DisplayName("should return DEFAULT for invalid URI")
        void shouldReturnDefaultForInvalidUri() {
            WebSocketConfig config = WebSocketConfig.fromUri("not-a-valid-uri");
            
            // Should return a config object (not crash)
            assertThat(config).isNotNull();
        }
        
        @Test
        @DisplayName("should handle missing port in URI")
        void shouldHandleMissingPort() {
            WebSocketConfig config = WebSocketConfig.fromUri("ws://myhost");
            
            assertThat(config.getHost()).isEqualTo("myhost");
            assertThat(config.getPort()).isEqualTo(8080); // Default port
        }
    }
    
    @Nested
    @DisplayName("Builder")
    class Builder {
        
        @Test
        @DisplayName("should create config with custom values")
        void shouldCreateWithCustomValues() {
            WebSocketConfig config = WebSocketConfig.builder()
                    .host("custom.host")
                    .port(9999)
                    .secure(true)
                    .path("/custom")
                    .autoConnect(false)
                    .autoReconnect(false)
                    .initialReconnectDelayMs(500L)
                    .maxReconnectDelayMs(60000L)
                    .reconnectBackoffMultiplier(1.5)
                    .maxReconnectAttempts(10)
                    .healthCheckIntervalMs(10000L)
                    .connectionTimeoutMs(5000L)
                    .debugLogging(true)
                    .build();
            
            assertThat(config.getHost()).isEqualTo("custom.host");
            assertThat(config.getPort()).isEqualTo(9999);
            assertThat(config.isSecure()).isTrue();
            assertThat(config.getPath()).isEqualTo("/custom");
            assertThat(config.isAutoConnect()).isFalse();
            assertThat(config.isAutoReconnect()).isFalse();
            assertThat(config.getInitialReconnectDelayMs()).isEqualTo(500L);
            assertThat(config.getMaxReconnectDelayMs()).isEqualTo(60000L);
            assertThat(config.getReconnectBackoffMultiplier()).isEqualTo(1.5);
            assertThat(config.getMaxReconnectAttempts()).isEqualTo(10);
            assertThat(config.getHealthCheckIntervalMs()).isEqualTo(10000L);
            assertThat(config.getConnectionTimeoutMs()).isEqualTo(5000L);
            assertThat(config.isDebugLogging()).isTrue();
        }
        
        @Test
        @DisplayName("should support toBuilder for modifications")
        void shouldSupportToBuilder() {
            WebSocketConfig original = WebSocketConfig.builder()
                    .host("original.host")
                    .port(1000)
                    .build();
            
            WebSocketConfig modified = original.toBuilder()
                    .port(2000)
                    .build();
            
            assertThat(original.getPort()).isEqualTo(1000);
            assertThat(modified.getPort()).isEqualTo(2000);
            assertThat(modified.getHost()).isEqualTo("original.host");
        }
    }
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {
        
        @Test
        @DisplayName("of() should create config with host and port")
        void ofShouldCreateWithHostAndPort() {
            WebSocketConfig config = WebSocketConfig.of("myhost", 5000);
            
            assertThat(config.getHost()).isEqualTo("myhost");
            assertThat(config.getPort()).isEqualTo(5000);
            assertThat(config.getUri()).isEqualTo("ws://myhost:5000");
        }
    }
    
    @Nested
    @DisplayName("With Methods")
    class WithMethods {
        
        @Test
        @DisplayName("withHost should create new config with different host")
        void withHostShouldWork() {
            WebSocketConfig original = WebSocketConfig.DEFAULT;
            WebSocketConfig modified = original.withHost("newhost");
            
            assertThat(original.getHost()).isEqualTo("localhost");
            assertThat(modified.getHost()).isEqualTo("newhost");
            assertThat(modified.getPort()).isEqualTo(original.getPort());
        }
        
        @Test
        @DisplayName("withPort should create new config with different port")
        void withPortShouldWork() {
            WebSocketConfig original = WebSocketConfig.DEFAULT;
            WebSocketConfig modified = original.withPort(3000);
            
            assertThat(original.getPort()).isEqualTo(8080);
            assertThat(modified.getPort()).isEqualTo(3000);
            assertThat(modified.getHost()).isEqualTo(original.getHost());
        }
    }
    
    @Test
    @DisplayName("toString should include URI and key settings")
    void toStringShouldBeInformative() {
        String str = WebSocketConfig.DEFAULT.toString();
        
        assertThat(str).contains("ws://localhost:8080");
        assertThat(str).contains("autoConnect=true");
        assertThat(str).contains("autoReconnect=true");
    }
}

