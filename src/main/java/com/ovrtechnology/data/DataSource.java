package com.ovrtechnology.data;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public interface DataSource {
    @Nullable
    JsonElement read(String classpathPath);

    default Map<Identifier, JsonElement> listJson(String directory) {
        return Collections.emptyMap();
    }
}
