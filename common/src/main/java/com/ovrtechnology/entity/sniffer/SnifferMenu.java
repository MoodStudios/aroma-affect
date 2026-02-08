package com.ovrtechnology.entity.sniffer;

import com.ovrtechnology.sniffernose.SnifferNoseItem;
import lombok.Getter;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SnifferMenu extends AbstractContainerMenu {

    private final SnifferContainer snifferContainer;
    @Getter
    private final Sniffer sniffer;

    public SnifferMenu(int containerId, Inventory playerInventory, SnifferContainer snifferContainer, Sniffer sniffer) {
        super(SnifferMenuRegistry.SNIFFER_MENU.get(), containerId);
        this.snifferContainer = snifferContainer;
        this.sniffer = sniffer;

        // Slot de silla (solo SADDLE)
        this.addSlot(new Slot(snifferContainer, SnifferContainer.SADDLE_SLOT, 8, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.SADDLE);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Slot de Enhanced Sniffer Nose (solo acepta SnifferNoseItem)
        this.addSlot(new Slot(snifferContainer, SnifferContainer.DECORATION_SLOT, 8, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SnifferNoseItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Inventario del jugador (3 filas) - posición estándar del GUI del caballo
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar del jugador
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack itemStack = slot.getItem();
            result = itemStack.copy();

            // Desde el contenedor del sniffer (slots 0-1)
            if (index < 2) {
                if (!this.moveItemStackTo(itemStack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Desde el inventario del jugador
            else {
                if (itemStack.is(Items.SADDLE)) {
                    if (!this.moveItemStackTo(itemStack, SnifferContainer.SADDLE_SLOT, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemStack.getItem() instanceof SnifferNoseItem) {
                    if (!this.moveItemStackTo(itemStack, SnifferContainer.DECORATION_SLOT, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Item no válido para los slots del sniffer, no hacer nada
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return snifferContainer.stillValid(player);
    }
}
