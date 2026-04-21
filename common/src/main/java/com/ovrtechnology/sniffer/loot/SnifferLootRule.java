package com.ovrtechnology.sniffer.loot;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class SnifferLootRule {

    @SerializedName("sniffer_nose")
    private String snifferNose;

    @SerializedName("biomes")
    private List<String> biomes;

    @SerializedName("always")
    private List<SnifferLootEntry> always;

    @SerializedName("pool")
    private Pool pool;

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Pool {
        @SerializedName("rolls")
        private IntRange rolls;

        @SerializedName("entries")
        private List<SnifferLootEntry> entries;
    }

    public boolean matchesNose(String noseFullId) {
        if (snifferNose == null || snifferNose.isEmpty() || "*".equals(snifferNose)) return true;
        return snifferNose.equals(noseFullId);
    }

    public boolean matchesBiome(Holder<Biome> biomeHolder) {
        if (biomes == null || biomes.isEmpty()) return true;
        for (String entry : biomes) {
            if (entry == null || entry.isEmpty()) continue;
            if (entry.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId == null) continue;
                TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, tagId);
                if (biomeHolder.is(tagKey)) return true;
            } else {
                ResourceLocation biomeId = ResourceLocation.tryParse(entry);
                if (biomeId == null) continue;
                ResourceLocation thisId = biomeHolder.unwrapKey().map(ResourceKey::location).orElse(null);
                if (biomeId.equals(thisId)) return true;
            }
        }
        return false;
    }
}
