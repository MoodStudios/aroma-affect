package com.ovrtechnology.guide;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

@UtilityClass
public final class AromaGuideRegistry {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ITEM);

    @Getter
    private static final RegistrySupplier<AromaGuideItem> AROMA_GUIDE =
            ITEMS.register("aroma_guide", AromaGuideItem::new);

    public static void init() {
        ITEMS.register();
        AromaAffect.LOGGER.info("Registered Aroma Guide item");
    }
}
