package com.ovrtechnology.variant;

import com.ovrtechnology.AromaAffect;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

public final class CustomNoseRegistry {

    private static Holder<Item> customNose;

    private CustomNoseRegistry() {}

    public static void register(BalmRegistrar.Scoped<Item> items) {
        AromaAffect.LOGGER.info("Initializing CustomNoseRegistry...");
        customNose = items.register(CustomNoseItem.ITEM_ID, identifier -> new CustomNoseItem(identifier));
    }

    public static Item getCustomNose() {
        return customNose != null && customNose.isBound() ? customNose.value() : null;
    }

    public static Holder<Item> getCustomNoseHolder() {
        return customNose;
    }
}
