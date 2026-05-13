package com.ovrtechnology.nose.accessory;

import net.blay09.mods.balm.Balm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Cross-platform abstraction over the "nose accessory" slot.
 *
 * <p>On Fabric the slot is always the vanilla HEAD slot.</p>
 * <p>On NeoForge with Curios installed, the slot is the Curios "face" slot;
 * without Curios it falls back to the HEAD slot via the {@code Equippable}
 * data component on {@code NoseItem}.</p>
 *
 * <p>The platform-specific implementation is resolved lazily through
 * {@link Balm#platformProxy()} at first use.</p>
 */
public final class NoseAccessory {

    public static final String SLOT_ID = "face";

    public interface Provider {
        ItemStack getEquipped(Player player);
        ItemStack equip(Player player, ItemStack stack);
        boolean hasSlot(Player player);
    }

    private static final Provider INSTANCE = Balm.<Provider>platformProxy()
            .withFabric("com.ovrtechnology.nose.accessory.fabric.NoseAccessoryImpl")
            .withNeoForge("com.ovrtechnology.nose.accessory.neoforge.NoseAccessoryImpl")
            .build();

    private NoseAccessory() {}

    public static ItemStack getEquipped(Player player) {
        return INSTANCE.getEquipped(player);
    }

    public static ItemStack equip(Player player, ItemStack stack) {
        return INSTANCE.equip(player, stack);
    }

    public static boolean hasSlot(Player player) {
        return INSTANCE.hasSlot(player);
    }
}
