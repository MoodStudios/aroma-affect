package com.ovrtechnology.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ovrtechnology.AromaAffect;
import dev.architectury.platform.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public final class TrackingHistoryData {

    private static final String LEGACY_CONFIG_FILE_NAME = "aromaaffect_history.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_HISTORY_SIZE = 100;

    private static TrackingHistoryData instance;

    private static String loadedWorldId;

    @Getter private List<HistoryEntry> history = new ArrayList<>();
    @Getter private List<SavedEntry> saved = new ArrayList<>();
    @Getter private List<BlacklistEntry> blacklist = new ArrayList<>();

    private TrackingHistoryData() {}

    public static TrackingHistoryData getInstance() {
        String currentWorldId = WorldIdentifier.getCurrentWorldId();

        if (instance != null && !currentWorldId.equals(loadedWorldId)) {
            AromaAffect.LOGGER.info(
                    "World ID changed from '{}' to '{}', reloading tracking history",
                    loadedWorldId,
                    currentWorldId);
            instance = null;
        }

        if (instance == null) {
            instance = load();
            loadedWorldId = currentWorldId;
        }
        return instance;
    }

    public static void invalidate() {
        if (instance != null) {
            instance.save();
        }
        instance = null;
        loadedWorldId = null;
    }

    public void addHistoryEntry(HistoryEntry entry) {
        history.addFirst(entry);
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeLast();
        }
        save();
    }

    public void removeHistoryEntry(int index) {
        if (index >= 0 && index < history.size()) {
            history.remove(index);
            save();
        }
    }

    public void saveEntry(HistoryEntry source, String customName) {
        SavedEntry entry =
                new SavedEntry(
                        source.targetId,
                        customName,
                        source.categoryId,
                        source.x,
                        source.y,
                        source.z,
                        source.dimension,
                        System.currentTimeMillis());
        saved.addFirst(entry);
        save();
    }

    public void renameSavedEntry(int index, String newName) {
        if (index >= 0 && index < saved.size()) {
            saved.get(index).customName = newName;
            save();
        }
    }

    public void removeSavedEntry(int index) {
        if (index >= 0 && index < saved.size()) {
            saved.remove(index);
            save();
        }
    }

    public void addToBlacklist(HistoryEntry source) {

        for (BlacklistEntry existing : blacklist) {
            if (existing.targetId.equals(source.targetId)
                    && existing.x == source.x
                    && existing.y == source.y
                    && existing.z == source.z) {
                return;
            }
        }
        BlacklistEntry entry =
                new BlacklistEntry(
                        source.targetId,
                        source.displayName,
                        source.categoryId,
                        source.x,
                        source.y,
                        source.z,
                        source.dimension,
                        System.currentTimeMillis());
        blacklist.addFirst(entry);
        save();
    }

    public void addToBlacklist(SavedEntry source) {
        for (BlacklistEntry existing : blacklist) {
            if (existing.targetId.equals(source.targetId)
                    && existing.x == source.x
                    && existing.y == source.y
                    && existing.z == source.z) {
                return;
            }
        }
        BlacklistEntry entry =
                new BlacklistEntry(
                        source.targetId,
                        source.customName,
                        source.categoryId,
                        source.x,
                        source.y,
                        source.z,
                        source.dimension,
                        System.currentTimeMillis());
        blacklist.addFirst(entry);
        save();
    }

    public void removeFromBlacklist(int index) {
        if (index >= 0 && index < blacklist.size()) {
            blacklist.remove(index);
            save();
        }
    }

    public boolean isBlacklisted(String targetId, int x, int y, int z) {
        for (BlacklistEntry entry : blacklist) {
            if (entry.targetId.equals(targetId) && entry.x == x && entry.y == y && entry.z == z) {
                return true;
            }
        }
        return false;
    }

    public boolean isSaved(String targetId, int x, int y, int z) {
        for (SavedEntry entry : saved) {
            if (entry.targetId.equals(targetId) && entry.x == x && entry.y == y && entry.z == z) {
                return true;
            }
        }
        return false;
    }

    private static TrackingHistoryData load() {
        String worldId = WorldIdentifier.getCurrentWorldId();

        if ("unknown".equals(worldId)) {
            AromaAffect.LOGGER.debug("World not ready yet, returning empty tracking history");
            return new TrackingHistoryData();
        }

        Path worldPath = getConfigPath();

        if (Files.exists(worldPath)) {
            TrackingHistoryData data = loadFrom(worldPath);
            if (data != null) return data;
        }

        Path legacyPath = Platform.getConfigFolder().resolve(LEGACY_CONFIG_FILE_NAME);
        if (Files.exists(legacyPath)) {
            TrackingHistoryData data = loadFrom(legacyPath);
            if (data != null) {
                data.save();
                try {
                    Path migratedPath =
                            legacyPath.resolveSibling(LEGACY_CONFIG_FILE_NAME + ".migrated");
                    Files.move(legacyPath, migratedPath);
                    AromaAffect.LOGGER.info(
                            "Migrated legacy tracking history to per-world file and renamed legacy file");
                } catch (IOException e) {

                    try {
                        Files.delete(legacyPath);
                    } catch (IOException ignored) {
                    }
                    AromaAffect.LOGGER.warn(
                            "Could not rename legacy history file, deleted it instead");
                }
                return data;
            }
        }

        AromaAffect.LOGGER.info("Creating default tracking history for world '{}'", worldId);
        TrackingHistoryData data = new TrackingHistoryData();
        data.save();
        return data;
    }

    private static TrackingHistoryData loadFrom(Path path) {
        try {
            String json = Files.readString(path);
            TrackingHistoryData data = GSON.fromJson(json, TrackingHistoryData.class);
            if (data != null) {
                if (data.history == null) data.history = new ArrayList<>();
                if (data.saved == null) data.saved = new ArrayList<>();
                if (data.blacklist == null) data.blacklist = new ArrayList<>();
                AromaAffect.LOGGER.info(
                        "Loaded tracking history from {} ({} history, {} saved, {} blacklisted)",
                        path.getFileName(),
                        data.history.size(),
                        data.saved.size(),
                        data.blacklist.size());
                return data;
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.warn(
                    "Failed to load tracking history from {}: {}", path, e.getMessage());
        }
        return null;
    }

    public void save() {

        if ("unknown".equals(WorldIdentifier.getCurrentWorldId())) {
            return;
        }
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(this));
            AromaAffect.LOGGER.debug("Saved tracking history");
        } catch (IOException e) {
            AromaAffect.LOGGER.error("Failed to save tracking history: {}", e.getMessage());
        }
    }

    private static Path getConfigPath() {
        String worldId = WorldIdentifier.getCurrentWorldId();
        return Platform.getConfigFolder()
                .resolve("aromaaffect")
                .resolve("history_" + worldId + ".json");
    }
}
