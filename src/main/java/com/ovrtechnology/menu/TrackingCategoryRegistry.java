package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.lookup.LookupType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TrackingCategoryRegistry {

    private static final Map<String, TrackingCategory> CATEGORIES = new LinkedHashMap<>();

    private TrackingCategoryRegistry() {
    }

    public static TrackingCategory register(TrackingCategory category) {
        TrackingCategory previous = CATEGORIES.put(category.getId(), category);
        if (previous != null) {
            AromaAffect.LOGGER.warn("Tracking category '{}' was registered more than once; replacing", category.getId());
        }
        return category;
    }

    public static TrackingCategory fromId(String id) {
        return id != null ? CATEGORIES.get(id) : null;
    }

    public static TrackingCategory fromLookupType(LookupType type) {
        if (type == null) {
            return null;
        }
        for (TrackingCategory category : CATEGORIES.values()) {
            if (category.getLookupType() == type) {
                return category;
            }
        }
        return null;
    }

    public static boolean isRegistered(String id) {
        return CATEGORIES.containsKey(id);
    }

    public static Collection<TrackingCategory> all() {
        return Collections.unmodifiableCollection(CATEGORIES.values());
    }

    public static int size() {
        return CATEGORIES.size();
    }

    public static void clear() {
        CATEGORIES.clear();
    }
}
