package com.ovrtechnology.history;

/**
 * Represents a blacklisted position that should be excluded from future searches.
 */
public class BlacklistEntry {

    public String targetId;
    public String displayName;
    public String categoryId;
    public int x, y, z;
    public long timestamp;

    /** No-arg constructor for Gson deserialization. */
    public BlacklistEntry() {}

    public BlacklistEntry(String targetId, String displayName, String categoryId,
                          int x, int y, int z, long timestamp) {
        this.targetId = targetId;
        this.displayName = displayName;
        this.categoryId = categoryId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }
}
