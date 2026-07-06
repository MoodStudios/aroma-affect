package com.ovrtechnology.menu;

public enum TrailDomain {
    BLOCK,
    BIOME,
    STRUCTURE;

    public float[] fallbackTrailColor() {
        return switch (this) {
            case STRUCTURE -> new float[]{1.0f, 0.8f, 0.2f};
            case BIOME -> new float[]{0.3f, 0.9f, 0.5f};
            case BLOCK -> new float[]{0.8f, 0.85f, 1.0f};
        };
    }
}
