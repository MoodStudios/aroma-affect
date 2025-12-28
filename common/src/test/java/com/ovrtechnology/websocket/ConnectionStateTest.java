package com.ovrtechnology.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ConnectionState}.
 */
@DisplayName("ConnectionState")
class ConnectionStateTest {
    
    @Test
    @DisplayName("DISCONNECTED should not be active or pending")
    void disconnectedShouldNotBeActiveOrPending() {
        assertThat(ConnectionState.DISCONNECTED.isActiveOrPending()).isFalse();
    }
    
    @Test
    @DisplayName("CONNECTING should be active or pending")
    void connectingShouldBeActiveOrPending() {
        assertThat(ConnectionState.CONNECTING.isActiveOrPending()).isTrue();
    }
    
    @Test
    @DisplayName("CONNECTED should be active or pending")
    void connectedShouldBeActiveOrPending() {
        assertThat(ConnectionState.CONNECTED.isActiveOrPending()).isTrue();
    }
    
    @Test
    @DisplayName("CONNECTION_FAILED should not be active or pending")
    void connectionFailedShouldNotBeActiveOrPending() {
        assertThat(ConnectionState.CONNECTION_FAILED.isActiveOrPending()).isFalse();
    }
    
    @Test
    @DisplayName("RECONNECTING should be active or pending")
    void reconnectingShouldBeActiveOrPending() {
        assertThat(ConnectionState.RECONNECTING.isActiveOrPending()).isTrue();
    }
    
    @Test
    @DisplayName("only CONNECTED should allow sending messages")
    void onlyConnectedShouldAllowMessages() {
        assertThat(ConnectionState.CONNECTED.canSendMessages()).isTrue();
        assertThat(ConnectionState.DISCONNECTED.canSendMessages()).isFalse();
        assertThat(ConnectionState.CONNECTING.canSendMessages()).isFalse();
        assertThat(ConnectionState.CONNECTION_FAILED.canSendMessages()).isFalse();
        assertThat(ConnectionState.RECONNECTING.canSendMessages()).isFalse();
    }
    
    @ParameterizedTest
    @EnumSource(ConnectionState.class)
    @DisplayName("all states should have non-empty display name")
    void allStatesShouldHaveDisplayName(ConnectionState state) {
        assertThat(state.getDisplayName()).isNotEmpty();
    }
    
    @Test
    @DisplayName("display names should be human-readable")
    void displayNamesShouldBeHumanReadable() {
        assertThat(ConnectionState.DISCONNECTED.getDisplayName()).isEqualTo("Disconnected");
        assertThat(ConnectionState.CONNECTING.getDisplayName()).isEqualTo("Connecting...");
        assertThat(ConnectionState.CONNECTED.getDisplayName()).isEqualTo("Connected");
        assertThat(ConnectionState.CONNECTION_FAILED.getDisplayName()).isEqualTo("Connection Failed");
        assertThat(ConnectionState.RECONNECTING.getDisplayName()).isEqualTo("Reconnecting...");
    }
}

