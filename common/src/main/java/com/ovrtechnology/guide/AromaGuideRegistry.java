package com.ovrtechnology.guide;

import com.ovrtechnology.AromaCraft;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * Registers the Aroma Guide item.
 */
@UtilityClass
public final class AromaGuideRegistry {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(AromaCraft.MOD_ID, Registries.ITEM);

    @Getter
    private static final RegistrySupplier<AromaGuideItem> AROMA_GUIDE =
            ITEMS.register("aroma_guide", AromaGuideItem::new);

    public static void init() {
        ITEMS.register();
        AromaCraft.LOGGER.info("Registered Aroma Guide item");
    }
}
