package com.ovrtechnology.entity.sniffer;

import com.ovrtechnology.AromaAffect;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.world.BalmMenuFactory;
import net.blay09.mods.balm.world.BalmMenuProvider;
import net.blay09.mods.balm.world.inventory.BalmMenuTypeRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Menu type registration for the Sniffer inventory screen.
 *
 * <p>In Architectury the menu used {@code MenuRegistry.ofExtended} to carry a
 * sniffer entity id in the open packet. Under Balm we register a plain
 * {@link MenuType} and pass the entity id through the player's
 * {@code carriedItem} channel via {@link Balm#networking()} / packet handling
 * in phase 6. The factory in {@link Player#openMenu(MenuProvider)} reads the
 * id from the menu open args provided by the platform.</p>
 */
public class SnifferMenuRegistry {

    public static Holder<MenuType<SnifferMenu>> SNIFFER_MENU;

    public static void registerMenus(BalmMenuTypeRegistrar menus) {
        // Balm wraps the package-private MenuType constructor. Payload is the
        // sniffer's entity id (varint) so the client can resolve the live
        // Sniffer reference -- SnifferScreen reads menu.getSniffer() to render
        // the entity preview.
        SNIFFER_MENU = menus.<SnifferMenu, Integer>register("sniffer", new SnifferMenuFactory()).asHolder();
    }

    public static void openSnifferMenu(ServerPlayer player, Sniffer sniffer) {
        Balm.networking().openMenu(player, new BalmMenuProvider<Integer>() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Sniffer Inventory");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
                return new SnifferMenu(containerId, inv, new SnifferContainer(sniffer), sniffer);
            }

            @Override
            public Integer getScreenOpeningData(ServerPlayer p) {
                return sniffer.getId();
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, Integer> getScreenStreamCodec() {
                return StreamCodec.of(
                        (buf, id) -> buf.writeVarInt(id),
                        buf -> buf.readVarInt()
                );
            }
        });
    }

    public static final class SnifferMenuFactory implements BalmMenuFactory<SnifferMenu, Integer> {
        @Override
        public SnifferMenu create(int containerId, Inventory inventory, Integer snifferId) {
            Entity e = inventory.player.level().getEntity(snifferId);
            Sniffer sniffer = e instanceof Sniffer s ? s : null;
            return new SnifferMenu(containerId, inventory, new SnifferContainer(sniffer), sniffer);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Integer> getStreamCodec() {
            return StreamCodec.of(
                    (buf, id) -> buf.writeVarInt(id),
                    buf -> buf.readVarInt()
            );
        }
    }

    public static void initClient() {
        // Screen registration moves to AromaAffectClient.initialize via
        // BalmClientRegistrars#menuScreens in phase 8.
    }
}
