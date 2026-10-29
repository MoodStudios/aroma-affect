package com.ovrtechnology.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ovrtechnology.menu.FeedbackConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Content availability")
class ContentAvailabilityTest {

    private static final Path DATA_ROOT = Path.of("src/main/resources/data/aromaaffect/aroma");

    @Test
    @DisplayName("versions compare numerically across 1.x and 26.x schemes")
    void versionsCompareNumerically() {
        assertThat(ContentAvailability.compareVersions(
                ContentAvailability.parseVersion("1.21.10"), ContentAvailability.parseVersion("1.21.4"))).isPositive();
        assertThat(ContentAvailability.compareVersions(
                ContentAvailability.parseVersion("26.2"), ContentAvailability.parseVersion("1.21.11"))).isPositive();
        assertThat(ContentAvailability.compareVersions(
                ContentAvailability.parseVersion("26.3"), ContentAvailability.parseVersion("26.3.0"))).isZero();
        assertThat(ContentAvailability.compareVersions(
                ContentAvailability.parseVersion("1.21.1"), ContentAvailability.parseVersion("26.2"))).isNegative();
    }

    @Test
    @DisplayName("content without min_minecraft is always available")
    void missingMinVersionIsAvailable() {
        assertThat(ContentAvailability.meetsMinVersion(null)).isTrue();
        assertThat(ContentAvailability.meetsMinVersion("")).isTrue();
        assertThat(ContentAvailability.meetsMinVersion(FeedbackConfig.MC_VERSION)).isTrue();
    }

    @Test
    @DisplayName("every min_minecraft in the bundled data is a plain dotted version")
    void bundledMinVersionsAreWellFormed() throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(DATA_ROOT)) {
            files = walk.filter(path -> path.toString().endsWith(".json")).toList();
        }
        for (Path file : files) {
            var element = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject json = element.getAsJsonObject();
            if (json.has("min_minecraft")) {
                assertThat(json.get("min_minecraft").getAsString())
                        .as("min_minecraft in %s", file)
                        .matches("\\d+(\\.\\d+){1,2}");
            }
        }
    }
}
