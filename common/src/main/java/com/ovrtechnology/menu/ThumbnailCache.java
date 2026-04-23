package com.ovrtechnology.menu;

import com.mojang.blaze3d.platform.NativeImage;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.util.Ids;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class ThumbnailCache {

    private static final Map<ResourceLocation, ResourceLocation> CACHE = new HashMap<>();
    private static final int ALPHA_THRESHOLD = 128;

    private ThumbnailCache() {}

    public static ResourceLocation getCutout(ResourceLocation original) {
        ResourceLocation cached = CACHE.get(original);
        if (cached != null) return cached;
        try {
            Resource resource =
                    Minecraft.getInstance().getResourceManager().getResourceOrThrow(original);
            try (InputStream stream = resource.open()) {
                NativeImage source = NativeImage.read(stream);
                NativeImage processed =
                        source.mappedCopy(
                                pixel -> {
                                    int a = (pixel >>> 24) & 0xFF;
                                    return a >= ALPHA_THRESHOLD ? pixel | 0xFF000000 : 0;
                                });
                source.close();

                String name = original.getPath().replace('/', '_').replace('.', '_');
                ResourceLocation cutoutLoc = Ids.mod("dynamic/cutout_" + name);
                DynamicTexture dyn = new DynamicTexture(processed);
                Minecraft.getInstance().getTextureManager().register(cutoutLoc, dyn);
                CACHE.put(original, cutoutLoc);
                return cutoutLoc;
            }
        } catch (IOException e) {
            AromaAffect.LOGGER.warn(
                    "Failed to load cutout thumbnail for {}: {}", original, e.getMessage());
            CACHE.put(original, original);
            return original;
        }
    }
}
