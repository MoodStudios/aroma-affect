package com.ovrtechnology.nose.accessory.fabric;

import com.ovrtechnology.nose.accessory.NoseAccessory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric platform impl: keeps noses on the vanilla HEAD slot (helmet).
 * Resolved through {@link com.ovrtechnology.nose.accessory.NoseAccessory}'s
 * {@code Balm.platformProxy()}.
 */
public final class NoseAccessoryImpl implements NoseAccessory.Provider {

    public NoseAccessoryImpl() {}

    @Override
    public ItemStack getEquipped(Player player) {
        if (player == null) return ItemStack.EMPTY;
        return player.getItemBySlot(EquipmentSlot.HEAD);
    }

    @Override
    public ItemStack equip(Player player, ItemStack stack) {
        if (player == null) return stack;
        ItemStack previous = player.getItemBySlot(EquipmentSlot.HEAD);
        player.setItemSlot(EquipmentSlot.HEAD, stack == null ? ItemStack.EMPTY : stack);
        return previous == null ? ItemStack.EMPTY : previous;
    }

    @Override
    public boolean hasSlot(Player player) {
        return player != null;
    }
}
