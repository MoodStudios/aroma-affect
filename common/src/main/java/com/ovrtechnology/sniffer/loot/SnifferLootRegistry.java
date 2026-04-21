package com.ovrtechnology.sniffer.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.data.DataSource;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SnifferLootRegistry {

    public static final String RULES_DIR = "aroma_sniffer_loot";

    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, SnifferLootRule> rules = new LinkedHashMap<>();

    private SnifferLootRegistry() {
    }

    public static Collection<SnifferLootRule> all() {
        return Collections.unmodifiableCollection(rules.values());
    }

    public static void reload(DataSource source) {
        rules.clear();
        Map<ResourceLocation, JsonElement> files = source.listJson(RULES_DIR);
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                SnifferLootRule rule = GSON.fromJson(entry.getValue(), SnifferLootRule.class);
                if (rule != null) {
                    rules.put(entry.getKey(), rule);
                }
            } catch (Exception e) {
                AromaAffect.LOGGER.error("Failed to parse sniffer loot rule {}: {}", entry.getKey(), e.getMessage());
            }
        }
        AromaAffect.LOGGER.info("SnifferLootRegistry loaded {} rules from {} file(s)", rules.size(), files.size());
    }
}
