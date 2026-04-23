package com.ovrtechnology.history;

public class BlacklistEntry {

    public String targetId;
    public String displayName;
    public String categoryId;
    public int x, y, z;
    public String dimension;
    public long timestamp;

    public BlacklistEntry() {}

    public BlacklistEntry(
            String targetId,
            String displayName,
            String categoryId,
            int x,
            int y,
            int z,
            String dimension,
            long timestamp) {
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
