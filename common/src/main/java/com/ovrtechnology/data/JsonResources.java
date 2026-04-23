package com.ovrtechnology.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ovrtechnology.AromaAffect;
import java.lang.reflect.Array;

public final class JsonResources {

    private JsonResources() {}

    @SuppressWarnings("unchecked")
    public static <T> T[] parseArrayOrWrapped(
            JsonElement element,
            String wrapperKey,
            Class<T[]> arrayClass,
            Gson gson,
            String sourceLabel) {
        T[] empty = (T[]) Array.newInstance(arrayClass.getComponentType(), 0);
        if (element == null) {
            return empty;
        }
        if (element.isJsonArray()) {
            T[] result = gson.fromJson(element, arrayClass);
            return result != null ? result : empty;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has(wrapperKey)) {
                T[] result = gson.fromJson(obj.get(wrapperKey), arrayClass);
                return result != null ? result : empty;
            }
        }
        AromaAffect.LOGGER.warn(
                "Invalid JSON format in {} (expected array or object with '{}' key)",
                sourceLabel,
                wrapperKey);
        return empty;
    }
}
