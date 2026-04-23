package com.ovrtechnology.lookup;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record LookupTarget(LookupType type, ResourceLocation resourceId) {

    public LookupTarget {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(resourceId, "resourceId cannot be null");
    }

    public String getCacheKey() {
        return type.getId() + ":" + resourceId.toString();
    }

    @Override
    public String toString() {
        return type.getId() + "[" + resourceId + "]";
    }
}
