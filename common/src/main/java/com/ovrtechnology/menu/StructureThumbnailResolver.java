package com.ovrtechnology.menu;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves structure IDs to thumbnail texture {@link ResourceLocation}s.
 *
 * <p>Path convention (allows resource-pack / modded structure extensibility):</p>
 * <pre>
 *   assets/aromaaffect/textures/gui/sprites/structures/{namespace}/{path}.png
 * </pre>
 *
 * <p>Results are cached in a small {@link HashMap}.
 * Call {@link #clearCache()} on resource-pack reload if needed.</p>
 */
public final class StructureThumbnailResolver {

    private static final ResourceLocation PLACEHOLDER = ResourceLocation.fromNamespaceAndPath(
            AromaAffect.MOD_ID, "textures/gui/sprites/structures/placeholder.png");

    private static final Map<ResourceLocation, ResourceLocation> CACHE = new HashMap<>();

    private StructureThumbnailResolver() {}

    /**
     * Resolve a structure ID to its thumbnail texture location.
     * Returns the structure-specific texture if it exists in the current resource packs,
     * otherwise falls back to the placeholder.
     */
    public static ResourceLocation resolve(ResourceLocation structureId) {
        return CACHE.computeIfAbsent(structureId, id -> {
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                    AromaAffect.MOD_ID,
                    "textures/gui/sprites/structures/" + id.getNamespace() + "/" + id.getPath() + ".png");

            if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
                return texture;
            }
            return PLACEHOLDER;
        });
    }

    /**
     * Clear the resolution cache. Call on resource-pack reload so newly
     * added / removed structure thumbnails are picked up.
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
