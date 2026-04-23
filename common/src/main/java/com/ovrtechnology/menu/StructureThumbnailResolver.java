package com.ovrtechnology.menu;

import com.ovrtechnology.util.Ids;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class StructureThumbnailResolver {

    private static final ResourceLocation PLACEHOLDER =
            Ids.mod("textures/gui/sprites/structures/placeholder.png");

    private static final Map<ResourceLocation, ResourceLocation> CACHE = new HashMap<>();

    private StructureThumbnailResolver() {}

    public static ResourceLocation resolve(ResourceLocation structureId) {
        return CACHE.computeIfAbsent(
                structureId,
                id -> {
                    ResourceLocation texture =
                            Ids.mod(
                                    "textures/gui/sprites/structures/"
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
