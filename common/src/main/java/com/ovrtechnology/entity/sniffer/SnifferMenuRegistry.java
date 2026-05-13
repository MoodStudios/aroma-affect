package com.ovrtechnology.entity.sniffer;

import com.ovrtechnology.AromaAffect;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.core.BalmRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
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

    public static Holder<MenuType<?>> SNIFFER_MENU;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerMenus(BalmRegistrar.Scoped<MenuType<?>> menus) {
        SNIFFER_MENU = menus.register("sniffer_menu", id ->
                new MenuType(SnifferMenuRegistry::createMenu, FeatureFlags.DEFAULT_FLAGS));
        AromaAffect.LOGGER.info("Registered sniffer menu type");
    }

    private static SnifferMenu createMenu(int containerId, Inventory inventory) {
        // Default constructor — the actual sniffer reference is hydrated by
        // openSnifferMenu via a Balm MenuProvider that supplies extra data.
        return new SnifferMenu(containerId, inventory, new SnifferContainer(null), null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void openSnifferMenu(ServerPlayer player, Sniffer sniffer) {
        Balm.networking().openMenu(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Sniffer Inventory");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
                return new SnifferMenu(containerId, inv, new SnifferContainer(sniffer), sniffer);
            }
        });
        // Phase 6: when Balm.openMenu is invoked the framework writes an extra-data buffer.
        // The sniffer reference is passed via a side channel (entity id sent in the menu
        // payload). For now we rely on `openMenu` directly carrying the Sniffer reference
        // through the MenuProvider; if the menu has to be re-instantiated client-side we'll
        // attach the sniffer id via Balm's extra-data hook in phase 6.
    }

    public static void initClient() {
        // Screen registration moves to AromaAffectClient.initialize via
        // BalmClientRegistrars#menuScreens in phase 8.
    }
}
