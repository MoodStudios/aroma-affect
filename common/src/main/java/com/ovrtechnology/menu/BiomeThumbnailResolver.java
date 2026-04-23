package com.ovrtechnology.menu;

import com.ovrtechnology.util.Ids;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class BiomeThumbnailResolver {

    private static final ResourceLocation PLACEHOLDER =
            Ids.mod("textures/gui/sprites/biomes/placeholder.png");

    private static final Map<ResourceLocation, ResourceLocation> CACHE = new HashMap<>();

    private BiomeThumbnailResolver() {}

    public static ResourceLocation resolve(ResourceLocation biomeId) {
        return CACHE.computeIfAbsent(
                biomeId,
                id -> {
                    ResourceLocation texture =
                            Ids.mod(
                                    "textures/gui/sprites/biomes/"
                                            + id.getNamespace()
                                            + "/"
                                            + id.getPath()
                                            + ".png");

                    if (Minecraft.getInstance()
                            .getResourceManager()
                            .getResource(texture)
                            .isPresent()) {
                        return texture;
                    }
                    return PLACEHOLDER;
                });
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
