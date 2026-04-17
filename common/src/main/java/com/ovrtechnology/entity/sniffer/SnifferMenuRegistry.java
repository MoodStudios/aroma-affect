package com.ovrtechnology.entity.sniffer;

import com.ovrtechnology.util.Texts;
import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public class SnifferMenuRegistry {

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(AromaAffect.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<SnifferMenu>> SNIFFER_MENU = MENUS.register("sniffer_menu",
            () -> MenuRegistry.ofExtended((containerId, inventory, buf) -> {
                int snifferId = buf.readInt();
                Sniffer sniffer = (Sniffer) inventory.player.level().getEntity(snifferId);
                if (sniffer == null) {
                    throw new IllegalStateException("Sniffer not found with id: " + snifferId);
                }
                return new SnifferMenu(containerId, inventory, new SnifferContainer(sniffer), sniffer);
            }));

    public static void init() {
        MENUS.register();
    }

    public static void initClient() {
        // Screen registration is done per-platform in Fabric/NeoForge modules
    }

    public static void openSnifferMenu(ServerPlayer player, Sniffer sniffer) {
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {
                buf.writeInt(sniffer.getId());
            }

            @Override
            public Component getDisplayName() {
                return Texts.lit("Sniffer Inventory");
            }

            @Override
            public SnifferMenu createMenu(int containerId, Inventory inventory, Player p) {
                return new SnifferMenu(containerId, inventory, new SnifferContainer(sniffer), sniffer);
            }
        });
    }
}
