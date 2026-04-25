package com.ovrtechnology.nose.accessory.fabric;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric platform impl: keeps noses on the vanilla HEAD slot (helmet). No accessory mod migration
 * for Fabric on this branch.
 */
public final class NoseAccessoryImpl {

    private NoseAccessoryImpl() {}

    public static ItemStack getEquipped(Player player) {
        if (player == null) return ItemStack.EMPTY;
        return player.getItemBySlot(EquipmentSlot.HEAD);
    }

    public static ItemStack equip(Player player, ItemStack stack) {
        if (player == null) return stack;
        ItemStack previous = player.getItemBySlot(EquipmentSlot.HEAD);
        player.setItemSlot(EquipmentSlot.HEAD, stack == null ? ItemStack.EMPTY : stack);
        return previous == null ? ItemStack.EMPTY : previous;
    }

    public static boolean hasSlot(Player player) {
        return player != null;
    }
}
