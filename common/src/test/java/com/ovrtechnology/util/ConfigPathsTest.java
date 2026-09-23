package com.ovrtechnology.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ConfigPaths}.
 */
@DisplayName("ConfigPaths")
class ConfigPathsTest {

    @Test
    @DisplayName("should fall back to the working-directory config folder without a Balm runtime")
    void shouldFallBackWithoutBalmRuntime() {
        assertThat(ConfigPaths.configDir()).isEqualTo(Path.of("config"));
        assertThat(ConfigPaths.resolve("aromaaffect", "sniffer_config.json"))
                .isEqualTo(Path.of("config", "aromaaffect", "sniffer_config.json"));
    }

    @Test
    @DisplayName("should copy a legacy file into the loader config directory")
    void shouldCopyLegacyFile(@TempDir Path tmp) throws IOException {
        Path legacy = tmp.resolve("cwd/config/aromaaffect_client.json");
        Path target = tmp.resolve("game/config/aromaaffect_client.json");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "{\"puffMode\":\"manual\"}");

        ConfigPaths.migrateLegacy(legacy, target);

        assertThat(target).hasContent("{\"puffMode\":\"manual\"}");
        assertThat(legacy).exists();
    }

    @Test
    @DisplayName("should never overwrite a file that already exists in the loader config directory")
    void shouldNotOverwriteExistingTarget(@TempDir Path tmp) throws IOException {
        Path legacy = tmp.resolve("cwd/config/aromaaffect_client.json");
        Path target = tmp.resolve("game/config/aromaaffect_client.json");
        Files.createDirectories(legacy.getParent());
        Files.createDirectories(target.getParent());
        Files.writeString(legacy, "old");
        Files.writeString(target, "current");

        ConfigPaths.migrateLegacy(legacy, target);

        assertThat(target).hasContent("current");
    }

    @Test
    @DisplayName("should do nothing when there is no legacy file")
    void shouldIgnoreMissingLegacyFile(@TempDir Path tmp) {
        Path target = tmp.resolve("game/config/aromaaffect_client.json");

        ConfigPaths.migrateLegacy(tmp.resolve("cwd/config/aromaaffect_client.json"), target);

        assertThat(target).doesNotExist();
        assertThat(target.getParent()).doesNotExist();
    }
}
