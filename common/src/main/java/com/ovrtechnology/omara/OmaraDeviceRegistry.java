package com.ovrtechnology.omara;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import lombok.experimental.UtilityClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

@UtilityClass
public final class OmaraDeviceRegistry {

    private static final String OMARA_DEVICE_ID = "omara_device";

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.BLOCK);

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.ITEM);

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<Block> OMARA_DEVICE = BLOCKS.register(OMARA_DEVICE_ID,
            () -> new OmaraDeviceBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .setId(ResourceKey.create(
                            Registries.BLOCK,
                            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, OMARA_DEVICE_ID)
                    ))
            ));

    public static final RegistrySupplier<BlockEntityType<OmaraDeviceBlockEntity>> OMARA_DEVICE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(OMARA_DEVICE_ID,
                    () -> new BlockEntityType<>(OmaraDeviceBlockEntity::new, java.util.Set.of(OMARA_DEVICE.get())));

    public static final RegistrySupplier<Item> OMARA_DEVICE_ITEM = ITEMS.register(OMARA_DEVICE_ID,
            () -> new BlockItem(OMARA_DEVICE.get(), new Item.Properties()
                    .setId(ResourceKey.create(
                            Registries.ITEM,
                            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, OMARA_DEVICE_ID)
                    ))
            ));

    public static final RegistrySupplier<MenuType<OmaraDeviceMenu>> OMARA_DEVICE_MENU =
            MENUS.register(OMARA_DEVICE_ID,
                    () -> new MenuType<>(OmaraDeviceMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void init() {
        BLOCKS.register();
        BLOCK_ENTITY_TYPES.register();
        ITEMS.register();
        MENUS.register();
        AromaAffect.LOGGER.info("OmaraDeviceRegistry initialized");
    }
}
