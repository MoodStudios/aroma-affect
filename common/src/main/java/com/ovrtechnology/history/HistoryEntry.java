package com.ovrtechnology.history;

/**
 * Represents a single tracking history entry.
 * Created when a path search finds a destination.
 */
public class HistoryEntry {

    public String targetId;
    public String displayName;
    public String categoryId;
    public int x, y, z;
    public String dimension;
    public long timestamp;

    /** No-arg constructor for Gson deserialization. */
    public HistoryEntry() {}

    public HistoryEntry(String targetId, String displayName, String categoryId,
                        int x, int y, int z, String dimension, long timestamp) {
        this.targetId = targetId;
        this.displayName = displayName;
        this.categoryId = categoryId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.timestamp = timestamp;
    }
}
