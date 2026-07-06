package com.ovrtechnology.omara;

import com.ovrtechnology.AromaAffect;
import java.util.Set;
import lombok.experimental.UtilityClass;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.blay09.mods.balm.world.BalmMenuFactory;
import net.blay09.mods.balm.world.inventory.BalmMenuTypeRegistrar;
import net.blay09.mods.balm.world.level.block.entity.BalmBlockEntityTypeRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

@UtilityClass
public final class OmaraDeviceRegistry {

    private static final String OMARA_DEVICE_ID = "omara_device";

    public static Holder<Block> OMARA_DEVICE;
    public static Holder<BlockEntityType<OmaraDeviceBlockEntity>> OMARA_DEVICE_BLOCK_ENTITY;
    public static Holder<Item> OMARA_DEVICE_ITEM;
    public static Holder<MenuType<OmaraDeviceMenu>> OMARA_DEVICE_MENU;

    public static void registerBlocks(BalmRegistrar.Scoped<Block> blocks) {
        OMARA_DEVICE = blocks.register(OMARA_DEVICE_ID, id -> new OmaraDeviceBlock(BlockBehaviour.Properties.of()
                .strength(1.0F)
                .noOcclusion()
                .setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    public static void registerItems(BalmRegistrar.Scoped<Item> items) {
        OMARA_DEVICE_ITEM = items.register(OMARA_DEVICE_ID, id -> new BlockItem(
                (Block) OMARA_DEVICE.value(),
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    }

    public static void registerBlockEntities(BalmBlockEntityTypeRegistrar blockEntities) {
        // Balm wraps the package-private BlockEntityType constructor so we can
        // register without a NeoForge/Fabric-specific extension. The
        // Supplier<Set<Block>> overload is used because OMARA_DEVICE.value() is
        // a plain Block, not a BlockLike (Balm's BlockLike interface is meant
        // for mods that wrap blocks in their own holder objects).
        OMARA_DEVICE_BLOCK_ENTITY = blockEntities.register(
                OMARA_DEVICE_ID,
                OmaraDeviceBlockEntity::new,
                () -> Set.of((Block) OMARA_DEVICE.value())
        ).asHolder();
    }

    public static void registerMenus(BalmMenuTypeRegistrar menus) {
        // The MenuType constructor is package-private in 26.1, so registration
        // goes through Balm's BalmMenuFactory wrapper. Payload is a BlockPos so
        // the client can locate the BlockEntity for any direct access; the slot
        // and data-slot syncing still happens through the vanilla container
        // packets, so the dummy fallback is safe if the BE is not yet loaded
        // client-side (which should never happen in practice -- the player
        // only opens this menu by interacting with the block).
        OMARA_DEVICE_MENU = menus.<OmaraDeviceMenu, BlockPos>register(
                OMARA_DEVICE_ID,
                new OmaraDeviceMenuFactory()
        ).asHolder();
    }

    public static final class OmaraDeviceMenuFactory implements BalmMenuFactory<OmaraDeviceMenu, BlockPos> {
        @Override
        public OmaraDeviceMenu create(int containerId, Inventory inventory, BlockPos pos) {
            BlockEntity be = inventory.player.level().getBlockEntity(pos);
            if (be instanceof OmaraDeviceBlockEntity omara) {
                return new OmaraDeviceMenu(containerId, inventory, omara, new SimpleContainerData(4));
            }
            return new OmaraDeviceMenu(containerId, inventory);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlockPos> getStreamCodec() {
            return StreamCodec.of(
                    (buf, pos) -> buf.writeBlockPos(pos),
                    buf -> buf.readBlockPos()
            );
        }
    }
}
