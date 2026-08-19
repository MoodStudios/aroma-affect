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
        int split = directory.lastIndexOf('/');
        String parent = split >= 0 ? directory.substring(0, split + 1) : "";
        String dirName = split >= 0 ? directory.substring(split + 1) : directory;
        JsonElement index = read(DataSourceInfo.ROOT + parent + "_indexes/" + dirName + ".json");
        if (index == null || !index.isJsonArray()) {
            return result;
        }
        JsonArray names = index.getAsJsonArray();
        for (JsonElement nameEl : names) {
            String name = nameEl.getAsString();
            JsonElement content = read(DataSourceInfo.ROOT + directory + "/" + name + ".json");
            if (content == null) {
                AromaAffect.LOGGER.warn("Indexed resource missing: {}/{}", directory, name);
                continue;
            }
            result.put(Identifier.fromNamespaceAndPath(AromaAffect.MOD_ID, name), content);
        }
        return result;
    }
}
