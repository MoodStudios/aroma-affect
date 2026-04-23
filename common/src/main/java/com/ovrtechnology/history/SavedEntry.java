package com.ovrtechnology.history;

public class SavedEntry {

    public String targetId;
    public String customName;
    public String categoryId;
    public int x, y, z;
    public String dimension;
    public long timestamp;

    public SavedEntry() {}

    public SavedEntry(
            String targetId,
            String customName,
            String categoryId,
            int x,
            int y,
            int z,
            String dimension,
            long timestamp) {
        this.targetId = targetId;
        this.customName = customName;
        this.categoryId = categoryId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.timestamp = timestamp;
    }
}
