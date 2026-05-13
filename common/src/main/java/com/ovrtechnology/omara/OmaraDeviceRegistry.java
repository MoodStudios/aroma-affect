package com.ovrtechnology.omara;

import com.ovrtechnology.AromaAffect;
import lombok.experimental.UtilityClass;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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

    public static Holder<Block> OMARA_DEVICE;
    public static Holder<BlockEntityType<?>> OMARA_DEVICE_BLOCK_ENTITY;
    public static Holder<Item> OMARA_DEVICE_ITEM;
    public static Holder<MenuType<?>> OMARA_DEVICE_MENU;

    public static void registerBlocks(BalmRegistrar.Scoped<Block> blocks) {
        OMARA_DEVICE = blocks.register(OMARA_DEVICE_ID, id -> new OmaraDeviceBlock(BlockBehaviour.Properties.of()
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    public static void registerItems(BalmRegistrar.Scoped<Item> items) {
        OMARA_DEVICE_ITEM = items.register(OMARA_DEVICE_ID, id -> new BlockItem(
                (Block) OMARA_DEVICE.value(),
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    }

    public static void registerBlockEntities(BalmRegistrar.Scoped<BlockEntityType<?>> blockEntities) {
        // TODO(balm-26.1): BlockEntityType.BlockEntitySupplier is package-private
        // in 26.1; the access widener wasn't honored. Use a Balm or
        // mod-loader specific factory instead. Stubbed for compile.
        AromaAffect.LOGGER.warn("OmaraDeviceRegistry.registerBlockEntities stubbed -- needs accessible BlockEntityType factory");
    }

    public static void registerMenus(BalmRegistrar.Scoped<MenuType<?>> menus) {
        // TODO(balm-26.1): MenuType constructor is package-private in 26.1.
        // Need a public factory for the open-menu pipeline. Stubbed.
        AromaAffect.LOGGER.warn("OmaraDeviceRegistry.registerMenus stubbed -- needs accessible MenuType factory");
    }
}
