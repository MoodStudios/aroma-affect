package com.ovrtechnology.guide;

import com.ovrtechnology.AromaCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * The Aroma Guide is a compass-like item that always points toward
 * the nearest village, regardless of village type.
 * <p>
 * It dynamically updates its target as the player moves, switching
 * to whichever village is closest at any given moment. While held,
 * the distance to the nearest village is displayed on the action bar.
 */
public class AromaGuideItem extends Item {

    public AromaGuideItem() {
        super(new Properties()
                .setId(ResourceKey.create(
                        Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(AromaCraft.MOD_ID, "aroma_guide")
                ))
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
        );
    }
}
