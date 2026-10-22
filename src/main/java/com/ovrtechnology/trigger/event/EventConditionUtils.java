package com.ovrtechnology.trigger.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EventConditionUtils {

    private EventConditionUtils() {}

    public static boolean getBoolean(JsonObject conditions, String key, boolean defaultValue) {
        if (conditions == null || !conditions.has(key)) return defaultValue;
        JsonElement el = conditions.get(key);
        try {
            return el.getAsBoolean();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double getDouble(JsonObject conditions, String key, double defaultValue) {
        if (conditions == null || !conditions.has(key)) return defaultValue;
        JsonElement el = conditions.get(key);
        try {
            return el.getAsDouble();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int getInt(JsonObject conditions, String key, int defaultValue) {
        if (conditions == null || !conditions.has(key)) return defaultValue;
        JsonElement el = conditions.get(key);
        try {
            return el.getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static List<String> getStringArray(JsonObject conditions, String key) {
        if (conditions == null || !conditions.has(key)) return Collections.emptyList();
        JsonElement el = conditions.get(key);
        if (!el.isJsonArray()) return Collections.emptyList();
        JsonArray arr = el.getAsJsonArray();
        List<String> out = new ArrayList<>(arr.size());
        for (JsonElement item : arr) {
            try {
                out.add(item.getAsString());
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    public static boolean biomePathMatchesAny(String biomePath, List<String> paths) {
        if (biomePath == null || paths == null || paths.isEmpty()) return false;
        for (String p : paths) {
            if (biomePath.contains(p)) return true;
        }
        return false;
    }
}
