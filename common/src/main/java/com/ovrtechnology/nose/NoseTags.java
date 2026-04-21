package com.ovrtechnology.nose;

import com.ovrtechnology.util.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class NoseTags {

    public static final TagKey<Item> NOSES = TagKey.create(Registries.ITEM, Ids.mod("noses"));

    private NoseTags() {
    }
}
