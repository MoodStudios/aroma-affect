package com.ovrtechnology.util;

import com.ovrtechnology.AromaAffect;
import net.blay09.mods.balm.Balm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves config files against the loader's config directory
 * (FabricLoader.getConfigDir() on Fabric, FMLPaths.CONFIGDIR on NeoForge).
 */
public final class ConfigPaths {

    // Where configs were written before: relative to the JVM working directory.
    private static final Path LEGACY_DIR = Path.of("config");

    private static final Set<Path> MIGRATED = ConcurrentHashMap.newKeySet();

    private ConfigPaths() {}

    public static Path configDir() {
        try {
            return Balm.config().getConfigDir().toPath();
        } catch (LinkageError | RuntimeException e) {
            // No Balm runtime (unit tests).
            return LEGACY_DIR;
        }
    }

    public static Path resolve(String first, String... more) {
        Path relative = Path.of(first, more);
        Path target = configDir().resolve(relative);
        if (MIGRATED.add(relative)) {
            migrateLegacy(LEGACY_DIR.resolve(relative), target);
        }
        return target;
    }

    // Only does anything when the launcher's working directory is not the game
    // directory; otherwise both paths are the same file. Copies instead of moving
    // so a downgrade still finds the old file.
    static void migrateLegacy(Path legacy, Path target) {
        if (Files.exists(target) || !Files.isRegularFile(legacy)) {
            return;
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(legacy, target);
            AromaAffect.LOGGER.info("Copied config {} to {}", legacy.toAbsolutePath(), target);
        } catch (IOException e) {
            AromaAffect.LOGGER.warn("Could not copy config {} to {}: {}", legacy.toAbsolutePath(), target, e.getMessage());
        }
    }
}
