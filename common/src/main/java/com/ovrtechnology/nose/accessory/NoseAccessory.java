package com.ovrtechnology.nose.accessory;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class NoseAccessory {

    public static final String SLOT_ID = "face";

    private NoseAccessory() {}

    @ExpectPlatform
    public static ItemStack getEquipped(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ItemStack equip(Player player, ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean hasSlot(Player player) {
        throw new AssertionError();
    }
}
