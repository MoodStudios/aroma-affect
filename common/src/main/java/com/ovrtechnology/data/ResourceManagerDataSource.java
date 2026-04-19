package com.ovrtechnology.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ovrtechnology.AromaAffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ResourceManagerDataSource implements DataSource {

    private final ResourceManager resourceManager;

    public ResourceManagerDataSource(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    @Nullable
    public JsonElement read(String classpathPath) {
        ResourceLocation loc = toResourceLocation(classpathPath);
        if (loc == null) {
            return null;
        }

        Optional<Resource> resource = resourceManager.getResource(loc);
        if (resource.isEmpty()) {
            return null;
        }

        try (BufferedReader reader = resource.get().openAsReader()) {
            return JsonParser.parseReader(reader);
        } catch (Exception e) {
            AromaAffect.LOGGER.error("Error reading resource {} ({}): {}",
                    loc, classpathPath, e.getMessage());
            return null;
        }
    }

    @Override
    public Map<ResourceLocation, JsonElement> listJson(String directory) {
        String prefix = directory + "/";
        Map<ResourceLocation, Resource> found = resourceManager.listResources(directory,
                loc -> loc.getPath().endsWith(".json"));
        Map<ResourceLocation, JsonElement> result = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            ResourceLocation loc = entry.getKey();
            String path = loc.getPath();
            if (!path.startsWith(prefix) || !path.endsWith(".json")) continue;
            String name = path.substring(prefix.length(), path.length() - ".json".length());
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement el = JsonParser.parseReader(reader);
                ResourceLocation keyLoc = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), name);
                result.put(keyLoc, el);
            } catch (Exception e) {
                AromaAffect.LOGGER.error("Failed to read {}: {}", loc, e.getMessage());
            }
        }
        return result;
    }

    @Nullable
    private static ResourceLocation toResourceLocation(String classpathPath) {
        if (classpathPath == null || classpathPath.isEmpty()) {
            return null;
        }
        String stripped = classpathPath.startsWith("data/")
                ? classpathPath.substring("data/".length())
                : classpathPath.startsWith("assets/")
                    ? classpathPath.substring("assets/".length())
                    : classpathPath;

        int slash = stripped.indexOf('/');
        if (slash <= 0 || slash == stripped.length() - 1) {
            return null;
        }
        String namespace = stripped.substring(0, slash);
        String path = stripped.substring(slash + 1);
        return ResourceLocation.tryBuild(namespace, path);
    }
}
