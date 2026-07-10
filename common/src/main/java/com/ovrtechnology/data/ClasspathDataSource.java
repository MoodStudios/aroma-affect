package com.ovrtechnology.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ovrtechnology.AromaAffect;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public enum ClasspathDataSource implements DataSource {
    INSTANCE;

    private static final String BASE = "data/" + AromaAffect.MOD_ID + "/";

    @Override
    @Nullable
    public JsonElement read(String classpathPath) {
        ClassLoader cl = ClasspathDataSource.class.getClassLoader();
        try (InputStream is = cl.getResourceAsStream(classpathPath)) {
            if (is == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error reading classpath resource: {}", classpathPath, e);
            return null;
        }
    }

    @Override
    public Map<Identifier, JsonElement> listJson(String directory) {
        Map<Identifier, JsonElement> result = new LinkedHashMap<>();
        JsonElement index = read(BASE + "_indexes/" + directory + ".json");
        if (index == null || !index.isJsonArray()) {
            return result;
        }
        JsonArray names = index.getAsJsonArray();
        for (JsonElement nameEl : names) {
            String name = nameEl.getAsString();
            JsonElement content = read(BASE + directory + "/" + name + ".json");
            if (content == null) {
                AromaAffect.LOGGER.warn("Indexed resource missing: {}/{}", directory, name);
                continue;
            }
            result.put(Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, name), content);
        }
        return result;
    }
}
