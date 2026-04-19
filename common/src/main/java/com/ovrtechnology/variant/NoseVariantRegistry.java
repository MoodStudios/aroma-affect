package com.ovrtechnology.variant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.DataSource;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class NoseVariantRegistry {

    public static final String VARIANTS_DIR = "aroma_nose_variants";

    private static final Gson GSON = new GsonBuilder().create();

    private static final Map<ResourceLocation, NoseVariant> variants = new LinkedHashMap<>();

    private NoseVariantRegistry() {
    }

    public static Map<ResourceLocation, NoseVariant> all() {
        return Collections.unmodifiableMap(variants);
    }

    public static Optional<NoseVariant> get(ResourceLocation id) {
        return Optional.ofNullable(variants.get(id));
    }

    public static void reload(DataSource source) {
        variants.clear();
        Map<ResourceLocation, JsonElement> discovered = source.listJson(VARIANTS_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : discovered.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                NoseVariant variant = GSON.fromJson(entry.getValue(), NoseVariant.class);
                if (variant == null) continue;
                if (variant.getId() == null || variant.getId().isEmpty()) {
                    variant.setId(id.toString());
                }
                variants.put(id, variant);
            } catch (Exception e) {
                AromaAffect.LOGGER.error("Failed to parse variant {}: {}", id, e.getMessage());
            }
        }
        AromaAffect.LOGGER.info("NoseVariantRegistry loaded {} variants", variants.size());
    }
}
