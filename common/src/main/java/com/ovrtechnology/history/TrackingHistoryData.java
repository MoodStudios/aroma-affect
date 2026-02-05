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

/**
 * Singleton persistence manager for tracking history, saved entries, and blacklist.
 * Data is stored in {@code aromaaffect_history.json} in the config folder.
 */
public final class TrackingHistoryData {

    private static final String CONFIG_FILE_NAME = "aromaaffect_history.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_HISTORY_SIZE = 100;

    private static TrackingHistoryData instance;

    private List<HistoryEntry> history = new ArrayList<>();
    private List<SavedEntry> saved = new ArrayList<>();
    private List<BlacklistEntry> blacklist = new ArrayList<>();

    private TrackingHistoryData() {}

    public static TrackingHistoryData getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    // ── History ──────────────────────────────────────────────────────────

    public List<HistoryEntry> getHistory() {
        return history;
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

    // ── Saved ────────────────────────────────────────────────────────────

    public List<SavedEntry> getSaved() {
        return saved;
    }

    public void saveEntry(HistoryEntry source, String customName) {
        SavedEntry entry = new SavedEntry(
                source.targetId,
                customName,
                source.categoryId,
                source.x, source.y, source.z,
                System.currentTimeMillis()
        );
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

    // ── Blacklist ────────────────────────────────────────────────────────

    public List<BlacklistEntry> getBlacklist() {
        return blacklist;
    }

    public void addToBlacklist(HistoryEntry source) {
        // Avoid duplicates
        for (BlacklistEntry existing : blacklist) {
            if (existing.targetId.equals(source.targetId)
                    && existing.x == source.x && existing.y == source.y && existing.z == source.z) {
                return;
            }
        }
        BlacklistEntry entry = new BlacklistEntry(
                source.targetId,
                source.displayName,
                source.categoryId,
                source.x, source.y, source.z,
                System.currentTimeMillis()
        );
        blacklist.addFirst(entry);
        save();
    }

    public void addToBlacklist(SavedEntry source) {
        for (BlacklistEntry existing : blacklist) {
            if (existing.targetId.equals(source.targetId)
                    && existing.x == source.x && existing.y == source.y && existing.z == source.z) {
                return;
            }
        }
        BlacklistEntry entry = new BlacklistEntry(
                source.targetId,
                source.customName,
                source.categoryId,
                source.x, source.y, source.z,
                System.currentTimeMillis()
        );
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
            if (entry.targetId.equals(targetId)
                    && entry.x == x && entry.y == y && entry.z == z) {
                return true;
            }
        }
        return false;
    }

    public boolean isSaved(String targetId, int x, int y, int z) {
        for (SavedEntry entry : saved) {
            if (entry.targetId.equals(targetId)
                    && entry.x == x && entry.y == y && entry.z == z) {
                return true;
            }
        }
        return false;
    }

    // ── Persistence ──────────────────────────────────────────────────────

    private static TrackingHistoryData load() {
        Path configPath = getConfigPath();

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                TrackingHistoryData data = GSON.fromJson(json, TrackingHistoryData.class);
                if (data != null) {
                    // Ensure lists are never null after deserialization
                    if (data.history == null) data.history = new ArrayList<>();
                    if (data.saved == null) data.saved = new ArrayList<>();
                    if (data.blacklist == null) data.blacklist = new ArrayList<>();
                    AromaAffect.LOGGER.info("Loaded tracking history ({} history, {} saved, {} blacklisted)",
                            data.history.size(), data.saved.size(), data.blacklist.size());
                    return data;
                }
            } catch (Exception e) {
                AromaAffect.LOGGER.warn("Failed to load tracking history, using defaults: {}", e.getMessage());
            }
        }

        AromaAffect.LOGGER.info("Creating default tracking history");
        TrackingHistoryData data = new TrackingHistoryData();
        data.save();
        return data;
    }

    public void save() {
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
        return Platform.getConfigFolder().resolve(CONFIG_FILE_NAME);
    }
}
