package com.ovrtechnology.tutorial.demo;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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

    /**
     * How many session folders can exist side by side. The first is the normal one; the others
     * are only used when the previous one could not be deleted (a file still open somewhere).
     */
    private static final int SESSION_FOLDER_SLOTS = 8;

    /** A station with no world for this long is taken to be stranded and gets a new game. */
    private static final int RECOVERY_TICKS = 20 * 60;

    private static boolean sessionActive = false;
    private static boolean editSessionActive = false;
    private static String sessionFolderName = SESSION_WORLD_NAME;

    /** A visitor session was started since launch, so the station is meant to keep one running. */
    private static boolean stationArmed = false;
    private static int ticksWithoutWorld = 0;

    private DemoWorldManager() {}

    private static Path savesPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("saves");
    }

    private static String sessionFolderName(int slot) {
        return slot == 0 ? SESSION_WORLD_NAME : SESSION_WORLD_NAME + "_" + slot;
    }

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
        return savesPath().resolve(sessionFolderName);
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
            stationArmed = false;
            loadWorld("OVR_Tutorial_Edit", false);
            return;
        }

        // Normal play: copy template to a session folder.
        // From here on the station is expected to keep itself running (see tickRecovery).
        stationArmed = true;
        ticksWithoutWorld = 0;
        DemoConfig.reload();

        String folder = clearSessionFolders();
        if (folder == null) {
            AromaAffect.LOGGER.error("No demo session folder could be cleared, every one is still in use");
            return;
        }
        try {
            copyDirectory(getTemplatePath(), savesPath().resolve(folder));
            sessionFolderName = folder;
            sessionActive = true;
            AromaAffect.LOGGER.info("Created demo session world from template");
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to create demo session world", e);
            return;
        }

        loadWorld(folder, true);
    }

    /**
     * Deletes the session folders left by earlier games and returns the name of the first one
     * that is really gone, or null when none is. A folder survives when something still has a
     * file of it open (an antivirus scan, a server that did not stop); the next slot is used
     * then instead of leaving the station without a game.
     */
    private static String clearSessionFolders() {
        String free = null;
        for (int slot = 0; slot < SESSION_FOLDER_SLOTS; slot++) {
            Path path = savesPath().resolve(sessionFolderName(slot));
            try {
                deleteDirectory(path);
            } catch (IOException e) {
                AromaAffect.LOGGER.warn("Could not delete {}: {}", path, e.toString());
            }
            if (free == null && !Files.exists(path)) {
                free = sessionFolderName(slot);
                if (slot > 0) {
                    AromaAffect.LOGGER.warn("Earlier demo session folders are still in use, playing from {}", free);
                }
            }
        }
        return free;
    }

    /**
     * Brings back a stranded station. Runs every client tick.
     * <p>
     * A restart that fails (the template could not be copied, the world would not open, the
     * server crashed) ends on the title screen or on a "disconnected" screen, with no world
     * behind it and nobody to press Play. Once a visitor session has been started, a minute
     * on one of those two starts a new game. Any other screen is left alone: it may belong to
     * a world that is still being opened, and a second start on top of it would fight it.
     */
    public static void tickRecovery(Minecraft mc) {
        net.minecraft.client.server.IntegratedServer server = mc.getSingleplayerServer();
        boolean loading = server != null && server.isRunning();
        boolean stranded = mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen
                || mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen;
        if (!stationArmed || !stranded || mc.level != null || loading || pendingDisconnect || waitingForServerStop) {
            ticksWithoutWorld = 0;
            return;
        }
        if (++ticksWithoutWorld < RECOVERY_TICKS) {
            return;
        }
        ticksWithoutWorld = 0;
        AromaAffect.LOGGER.error("The demo station had no world for {} seconds, starting a new game", RECOVERY_TICKS / 20);
        startSession(false);
    }

    /**
     * Cleans up the session world after exit.
     */
    public static void cleanupSession() {
        boolean wasActive = sessionActive;
        sessionActive = false;
        boolean deleted = false;
        for (int slot = 0; slot < SESSION_FOLDER_SLOTS; slot++) {
            Path sessionPath = savesPath().resolve(sessionFolderName(slot));
            if (Files.exists(sessionPath)) {
                try {
                    deleteDirectory(sessionPath);
                    deleted = true;
                } catch (IOException e) {
                    AromaAffect.LOGGER.error("Failed to delete demo session world", e);
                }
            }
        }
        if (deleted || wasActive) {
            AromaAffect.LOGGER.info("Deleted demo session world");
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

    // The world of the session that is ending. Its files cannot be deleted or replaced
    // until its server has written the last of them.
    private static final long SERVER_STOP_WARNING_NANOS = 30_000_000_000L;
    private static boolean waitingForServerStop = false;
    private static MinecraftServer stoppingServer = null;
    private static long serverStopStarted = 0L;
    private static long serverStopNextWarning = 0L;

    /**
     * Starts waiting for the server of the session that is ending. Pass null when there is none.
     */
    public static void beginServerStop(MinecraftServer server) {
        waitingForServerStop = true;
        stoppingServer = server;
        serverStopStarted = System.nanoTime();
        serverStopNextWarning = SERVER_STOP_WARNING_NANOS;
    }

    public static boolean isWaitingForServerStop() {
        return waitingForServerStop;
    }

    /**
     * True once the old server has saved and closed its world. Ends the wait.
     * <p>
     * There is no giving up: a second server started beside one that is still alive would
     * share its world files and the loader's per-server state, which is worse than a station
     * that waits. A server that never stops shows in the log every 30 seconds.
     */
    public static boolean serverStopFinished() {
        if (stoppingServer != null && !stoppingServer.isShutdown()) {
            long waited = System.nanoTime() - serverStopStarted;
            if (waited >= serverStopNextWarning) {
                serverStopNextWarning += SERVER_STOP_WARNING_NANOS;
                AromaAffect.LOGGER.error("The demo server has not stopped after {} seconds, the next game cannot start until it does",
                        waited / 1_000_000_000L);
            }
            return false;
        }
        AromaAffect.LOGGER.info("Demo restart: server stopped after {} ms",
                (System.nanoTime() - serverStopStarted) / 1_000_000L);
        waitingForServerStop = false;
        stoppingServer = null;
        return true;
    }

    /**
     * Schedules a new game to start after returning to the title screen.
     */
    public static void scheduleNewGame() {
        pendingNewGame = true;
    }

    /**
     * Leaves the current session and starts a fresh one from the template.
     * Only sets flags, so it is safe to call from a tick or a screen.
     */
    public static void restartSession() {
        scheduleNewGame();
        scheduleDisconnect();
    }

    /** True while a visitor-facing session is running (not the template editor). */
    public static boolean isStationSession() {
        return sessionActive && !editSessionActive;
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
            // Not the loading screen: nothing is loading. The title screen is where
            // tickRecovery picks a station session up again.
            sessionActive = false;
            editSessionActive = false;
            mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
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
        } catch (java.io.UncheckedIOException e) {
            // The walk itself failed (a file vanished or became unreadable half way)
            throw e.getCause();
        }
    }
}
