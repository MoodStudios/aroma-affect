package com.ovrtechnology.sniffer;

import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SnifferContainer implements Container {
    private final Sniffer sniffer;
    private final SnifferTamingData data;

    public static final int SADDLE_SLOT = 0;
    public static final int DECORATION_SLOT = 1;
    public static final int CONTAINER_SIZE = 2;

    public SnifferContainer(Sniffer sniffer) {
        this.sniffer = sniffer;
        this.data = SnifferTamingData.get(sniffer.getUUID());
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return data.saddleItem.isEmpty() && data.decorationItem.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == SADDLE_SLOT ? data.saddleItem :
                slot == DECORATION_SLOT ? data.decorationItem :
                        ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack stack = getItem(slot);

        if (!stack.isEmpty()) {
            ItemStack result = stack.split(count);
            setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);

        if (slot == SADDLE_SLOT) {
            data.saddleItem = ItemStack.EMPTY;
        } else if (slot == DECORATION_SLOT) {
            data.decorationItem = ItemStack.EMPTY;
        }
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == SADDLE_SLOT) {
            // Solo acepta sillas de montar
            if (!stack.isEmpty() && !stack.is(Items.SADDLE)) {
                return;
            }
            data.saddleItem = stack.copy();
        } else if (slot == DECORATION_SLOT) {
            // Acepta cualquier cosa (tu placeholder)
            data.decorationItem = stack.copy();
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        // Sincronización automática
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return !sniffer.isDeadOrDying() &&
                player.distanceToSqr(sniffer) <= 64.0;
    }

    @Override
    public void clearContent() {
        data.saddleItem = ItemStack.EMPTY;
        data.decorationItem = ItemStack.EMPTY;
    }

    public boolean hasSaddle() {
        return !data.saddleItem.isEmpty() && data.saddleItem.is(Items.SADDLE);
    }

}