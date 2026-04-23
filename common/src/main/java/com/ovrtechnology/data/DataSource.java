package com.ovrtechnology.data;

import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface DataSource {
    @Nullable
    JsonElement read(String classpathPath);

    default Map<ResourceLocation, JsonElement> listJson(String directory) {
        return Collections.emptyMap();
    }
}
