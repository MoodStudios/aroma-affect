package com.ovrtechnology.menu;

import com.ovrtechnology.util.Ids;
import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves biome IDs to thumbnail texture {@link ResourceLocation}s.
 *
 * <p>Path convention (allows resource-pack / modded biome extensibility):</p>
 * <pre>
 *   assets/aromaaffect/textures/gui/sprites/biomes/{namespace}/{path}.png
 * </pre>
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code minecraft:plains} → {@code textures/gui/sprites/biomes/minecraft/plains.png}</li>
 *   <li>{@code mymod:enchanted_forest} → {@code textures/gui/sprites/biomes/mymod/enchanted_forest.png}</li>
 * </ul>
 *
 * <p>Results are cached in a small {@link HashMap} (~62 entries for vanilla).
 * Call {@link #clearCache()} on resource-pack reload if needed.</p>
 */
public final class BiomeThumbnailResolver {

    private static final ResourceLocation PLACEHOLDER = Ids.mod("textures/gui/sprites/biomes/placeholder.png");

    private static final Map<ResourceLocation, ResourceLocation> CACHE = new HashMap<>();

    private BiomeThumbnailResolver() {}

    /**
     * Resolve a biome ID to its thumbnail texture location.
     * Returns the biome-specific texture if it exists in the current resource packs,
     * otherwise falls back to the placeholder.
     */
    public static ResourceLocation resolve(ResourceLocation biomeId) {
        return CACHE.computeIfAbsent(biomeId, id -> {
            ResourceLocation texture = Ids.mod("textures/gui/sprites/biomes/" + id.getNamespace() + "/" + id.getPath() + ".png");

            // Check if the texture actually exists in loaded resource packs
            if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
                return texture;
            }
            return PLACEHOLDER;
        });
    }

    /**
     * Clear the resolution cache. Call on resource-pack reload so newly
     * added / removed biome thumbnails are picked up.
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
