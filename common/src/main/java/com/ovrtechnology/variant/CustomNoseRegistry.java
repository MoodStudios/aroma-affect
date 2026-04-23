package com.ovrtechnology.variant;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public final class CustomNoseRegistry {

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ITEM);

    @Getter
    private static final RegistrySupplier<CustomNoseItem> CUSTOM_NOSE =
            ITEMS.register(CustomNoseItem.ITEM_ID, CustomNoseItem::new);

    private CustomNoseRegistry() {}

    public static void init() {
        AromaAffect.LOGGER.info("Initializing CustomNoseRegistry...");
        ITEMS.register();
    }
}
