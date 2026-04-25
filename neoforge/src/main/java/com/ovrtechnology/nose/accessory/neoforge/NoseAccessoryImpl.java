package com.ovrtechnology.nose.accessory.neoforge;

import com.ovrtechnology.nose.accessory.NoseAccessory;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public final class NoseAccessoryImpl {

    private NoseAccessoryImpl() {}

    public static ItemStack getEquipped(Player player) {
        if (player == null) return ItemStack.EMPTY;
        IItemHandlerModifiable handler = resolveSlot(player);
        if (handler == null) return ItemStack.EMPTY;
        ItemStack stack = handler.getStackInSlot(0);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static ItemStack equip(Player player, ItemStack stack) {
        if (player == null) return stack;
        IItemHandlerModifiable handler = resolveSlot(player);
        if (handler == null) return stack;
        ItemStack previous = handler.getStackInSlot(0);
        handler.setStackInSlot(0, stack == null ? ItemStack.EMPTY : stack);
        return previous == null ? ItemStack.EMPTY : previous;
    }

    public static boolean hasSlot(Player player) {
        return resolveSlot(player) != null;
    }

    private static IItemHandlerModifiable resolveSlot(Player player) {
        Optional<ICuriosItemHandler> maybeHandler = CuriosApi.getCuriosInventory(player);
        if (maybeHandler.isEmpty()) return null;
        ICuriosItemHandler handler = maybeHandler.get();
        Optional<ICurioStacksHandler> stacks = handler.getStacksHandler(NoseAccessory.SLOT_ID);
        if (stacks.isEmpty()) return null;
        return stacks.get().getStacks();
    }
}
