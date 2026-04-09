package com.ovrtechnology.tutorial.demo;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Manages the demo world lifecycle:
 * - Copy template → saves folder
 * - Load the world
 * - Delete on exit
 */
public final class DemoWorldManager {

    private static final String SESSION_WORLD_NAME = "OVR_Tutorial_Session";
    private static final String TEMPLATE_FOLDER_NAME = "aromaaffect_template";

    private static boolean sessionActive = false;
    private static boolean editSessionActive = false;

    private DemoWorldManager() {}

    /**
     * Gets the template folder path (next to the game directory).
     */
    public static Path getTemplatePath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(TEMPLATE_FOLDER_NAME);
    }

    /**
     * Gets the session world path in saves.
     */
    public static Path getSessionSavePath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("saves").resolve(SESSION_WORLD_NAME);
    }

    /**
     * Checks if the template map exists.
     */
    public static boolean hasTemplate() {
        Path template = getTemplatePath();
        return Files.isDirectory(template) && Files.exists(template.resolve("level.dat"));
    }

    /**
     * Starts a demo session: copy template → load world.
     *
     * @param editMode if true, load the template directly (no copy, no cleanup)
     */
    public static void startSession(boolean editMode) {
        Minecraft mc = Minecraft.getInstance();

        if (!hasTemplate()) {
            AromaAffect.LOGGER.error("Template map not found at {}", getTemplatePath());
            return;
        }

        if (editMode) {
            // Edit mode: load template directly as a world
            // Copy to saves with a permanent name so it can be edited
            Path editSavePath = mc.gameDirectory.toPath().resolve("saves").resolve("OVR_Tutorial_Edit");
            try {
                if (Files.exists(editSavePath)) {
                    deleteDirectory(editSavePath);
                }
                copyDirectory(getTemplatePath(), editSavePath);
                AromaAffect.LOGGER.info("Copied template to edit world");
            } catch (IOException e) {
                AromaAffect.LOGGER.error("Failed to copy template for editing", e);
                return;
            }

            editSessionActive = true;
            loadWorld("OVR_Tutorial_Edit", false);
            return;
        }

        // Normal play: copy template to session folder
        Path sessionPath = getSessionSavePath();
        try {
            // Clean any leftover session
            if (Files.exists(sessionPath)) {
                deleteDirectory(sessionPath);
            }

            copyDirectory(getTemplatePath(), sessionPath);
            sessionActive = true;
            AromaAffect.LOGGER.info("Created demo session world from template");
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to create demo session world", e);
            return;
        }

        loadWorld(SESSION_WORLD_NAME, true);
    }

    /**
     * Cleans up the session world after exit.
     */
    public static void cleanupSession() {
        sessionActive = false;
        Path sessionPath = getSessionSavePath();
        if (Files.exists(sessionPath)) {
            try {
                deleteDirectory(sessionPath);
                AromaAffect.LOGGER.info("Deleted demo session world");
            } catch (IOException e) {
                AromaAffect.LOGGER.error("Failed to delete demo session world", e);
            }
        }
    }

    /**
     * Cleans up the edit world after exit.
     */
    public static void cleanupEditSession() {
        editSessionActive = false;
        Path editPath = Minecraft.getInstance().gameDirectory.toPath().resolve("saves").resolve("OVR_Tutorial_Edit");
        if (Files.exists(editPath)) {
            try {
                deleteDirectory(editPath);
                AromaAffect.LOGGER.info("Deleted edit session world");
            } catch (IOException e) {
                AromaAffect.LOGGER.error("Failed to delete edit session world", e);
            }
        }
    }

    /**
     * Saves the entire world (data + chunks + entities) to the template.
     */
    public static void saveDataToTemplate(net.minecraft.server.level.ServerLevel level) throws IOException {
        Path worldSavePath = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        Path template = getTemplatePath();

        AromaAffect.LOGGER.info("Saving full world to template: {} -> {}", worldSavePath, template);

        // Delete old template and copy entire world
        if (Files.exists(template)) {
            deleteDirectory(template);
        }
        copyDirectory(worldSavePath, template);

        AromaAffect.LOGGER.info("Full world saved to template ({} files)", countFiles(template));
    }

    private static int countFiles(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            return (int) stream.filter(Files::isRegularFile).count();
        }
    }

    /**
     * Saves the edit world back to the template folder.
     * Called when exiting edit mode.
     */
    public static void saveEditToTemplate() {
        Path editSavePath = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("saves").resolve("OVR_Tutorial_Edit");
        if (!Files.exists(editSavePath)) {
            return;
        }

        try {
            Path template = getTemplatePath();
            if (Files.exists(template)) {
                deleteDirectory(template);
            }
            copyDirectory(editSavePath, template);
            AromaAffect.LOGGER.info("Saved edit world back to template");
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to save edit world to template", e);
        }
    }

    /**
     * Schedules cleanup for after the server fully stops.
     * Uses a flag so cleanup runs when we return to the title screen.
     */
    public static void scheduleCleanup() {
        if (sessionActive || editSessionActive) {
            pendingCleanup = true;
        }
    }

    private static boolean pendingCleanup = false;

    /**
     * Runs pending cleanup if any. Called from title screen init.
     * For edit sessions: saves changes back to template before cleanup.
     */
    public static void runPendingCleanup() {
        if (!pendingCleanup) return;
        pendingCleanup = false;

        if (editSessionActive) {
            editSessionActive = false;
            saveEditToTemplate();
            AromaAffect.LOGGER.info("Edit session ended — changes saved to template");
        }

        cleanupSession();
    }

    public static boolean isSessionActive() {
        return sessionActive;
    }

    public static boolean isEditSession() {
        return editSessionActive;
    }

    private static boolean pendingDisconnect = false;
    private static boolean pendingNewGame = false;

    /**
     * Schedules a disconnect. Processed by TutorialFinishScreen.tick() which
     * runs outside of ClientTickEvent (avoids deadlock).
     */
    public static void scheduleDisconnect() {
        pendingDisconnect = true;
    }

    public static boolean isPendingDisconnect() {
        return pendingDisconnect;
    }

    public static void clearPendingDisconnect() {
        pendingDisconnect = false;
    }

    /**
     * Schedules a new game to start after returning to the title screen.
     */
    public static void scheduleNewGame() {
        pendingNewGame = true;
    }

    /**
     * Checks and runs pending new game. Called from title screen init.
     */
    public static void runPendingNewGame() {
        if (!pendingNewGame) return;
        pendingNewGame = false;
        startSession(false);
    }

    public static String getSessionWorldName() {
        return SESSION_WORLD_NAME;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────────

    private static void loadWorld(String worldName, boolean isSession) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new GenericMessageScreen(Component.literal("Loading world...")));

        // Use Minecraft's built-in world loading
        LevelStorageSource levelSource = mc.getLevelSource();
        try {
            LevelStorageSource.LevelStorageAccess access = levelSource.createAccess(worldName);
            // Close the access — we just needed to verify it works
            access.close();
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Failed to access world {}", worldName, e);
            return;
        }

        // Load the world through Minecraft's standard flow
        mc.createWorldOpenFlows().openWorld(worldName, () -> {
            mc.setScreen(null);
        });
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Skip session.lock to avoid conflicts
                if (file.getFileName().toString().equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }
}
