package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

/**
 * Registers the Aroma Guide item.
 *
 * <p>Registered through {@code registrars.registrar(Registries.ITEM, AromaGuideRegistry::register)}
 * from {@link AromaAffect#initialize(net.blay09.mods.balm.core.BalmRegistrars)}.</p>
 */
@UtilityClass
public final class AromaGuideRegistry {

    @Getter
    private static Holder<Item> aromaGuide;

    @SuppressWarnings("unchecked")
    public static void register(BalmRegistrar.Scoped<Item> items) {
        aromaGuide = items.register("aroma_guide", id -> new AromaGuideItem());
        AromaAffect.LOGGER.info("Registered Aroma Guide item");
    }

    public static AromaGuideItem getItem() {
        return aromaGuide != null && aromaGuide.isBound() ? (AromaGuideItem) aromaGuide.value() : null;
    }
}
