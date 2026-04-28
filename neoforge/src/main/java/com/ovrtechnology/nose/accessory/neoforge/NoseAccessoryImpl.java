package com.ovrtechnology.nose.accessory.neoforge;

import com.ovrtechnology.nose.accessory.NoseAccessory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * NeoForge platform impl. Curios is an optional dependency: when present, the "face" accessory slot
 * is preferred so noses live alongside helmets; when absent, everything falls back to the vanilla
 * HEAD slot via the Equippable component on {@code NoseItem}.
 *
 * <p>All Curios API calls go through {@link CuriosBridge}, an inner class that is only loaded if
 * {@code curios} is on the modlist — keeping the JVM from resolving {@code top.theillusivec4.*}
 * symbols when the mod is missing.
 */
public final class NoseAccessoryImpl {

    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    private NoseAccessoryImpl() {}

    public static ItemStack getEquipped(Player player) {
        if (player == null) return ItemStack.EMPTY;
        if (CURIOS_LOADED) {
            ItemStack curios = CuriosBridge.get(player);
            if (!curios.isEmpty()) return curios;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head == null ? ItemStack.EMPTY : head;
    }

    public static ItemStack equip(Player player, ItemStack stack) {
        if (player == null) return stack;
        if (CURIOS_LOADED) {
            ItemStack previous = CuriosBridge.equip(player, stack);
            if (previous != null) return previous;
        }
        ItemStack previous = player.getItemBySlot(EquipmentSlot.HEAD);
        player.setItemSlot(EquipmentSlot.HEAD, stack == null ? ItemStack.EMPTY : stack);
        return previous == null ? ItemStack.EMPTY : previous;
    }

    public static boolean hasSlot(Player player) {
        return player != null;
    }

    /**
     * Wrapper around the Curios API. Loaded lazily — JVM only resolves the {@code
     * top.theillusivec4.curios.*} class references when this class is first touched, which only
     * happens after the {@link #CURIOS_LOADED} guard succeeds.
     */
    private static final class CuriosBridge {

        static ItemStack get(Player player) {
            var maybeHandler = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player);
            if (maybeHandler.isEmpty()) return ItemStack.EMPTY;
            var stacks = maybeHandler.get().getStacksHandler(NoseAccessory.SLOT_ID);
            if (stacks.isEmpty()) return ItemStack.EMPTY;
            ItemStack stack = stacks.get().getStacks().getStackInSlot(0);
            return stack == null ? ItemStack.EMPTY : stack;
        }

        static ItemStack equip(Player player, ItemStack stack) {
            var maybeHandler = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player);
            if (maybeHandler.isEmpty()) return null;
            var stacks = maybeHandler.get().getStacksHandler(NoseAccessory.SLOT_ID);
            if (stacks.isEmpty()) return null;
            var handler = stacks.get().getStacks();
            ItemStack previous = handler.getStackInSlot(0);
            handler.setStackInSlot(0, stack == null ? ItemStack.EMPTY : stack);
            return previous == null ? ItemStack.EMPTY : previous;
        }
    }
}
