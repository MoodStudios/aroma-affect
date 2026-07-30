package com.ovrtechnology.menu;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeedbackClient")
class FeedbackClientTest {

    @Test
    @DisplayName("should fail gracefully when no signing secret is configured")
    void shouldFailGracefullyWithoutSigningSecret() {
        Assumptions.assumeFalse(
                FeedbackClient.isConfigured(),
                "This check only applies to development builds without an injected secret");

        assertThat(FeedbackClient.submit("Test feedback", "", true))
                .succeedsWithin(Duration.ofSeconds(1))
                .isEqualTo(false);
    }
}
