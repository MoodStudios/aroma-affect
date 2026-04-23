package com.ovrtechnology.lookup;

import lombok.Getter;

@Getter
public enum LookupType {
    BIOME("biome"),

    STRUCTURE("structure"),

    BLOCK("block"),

    FLOWER("flower");

    private final String id;

    LookupType(String id) {
        this.id = id;
    }

    public static LookupType fromId(String id) {
        for (LookupType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
