package com.ovrtechnology.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ovrtechnology.AromaAffect;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

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
}
