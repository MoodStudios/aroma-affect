package com.ovrtechnology.omara;

import com.ovrtechnology.scentitem.ScentItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OmaraDeviceMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    // Client-side constructor (used by MenuType factory)
    public OmaraDeviceMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainerData(4));
    }

    // Server-side constructor
    public OmaraDeviceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(OmaraDeviceRegistry.OMARA_DEVICE_MENU.get(), containerId);
        this.container = container;
        this.data = data;
        this.access = container instanceof OmaraDeviceBlockEntity be && be.getLevel() != null
                ? ContainerLevelAccess.create(be.getLevel(), be.getBlockPos())
                : ContainerLevelAccess.NULL;

        checkContainerSize(container, 1);
        checkContainerDataCount(data, 4);
        container.startOpen(playerInventory.player);

        // Single device slot at center of dispenser grid — capsule-only
        this.addSlot(new CapsuleSlot(container, 0, 80, 35));

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int getCooldownTicks() {
        return this.data.get(0);
    }

    public int getMaxCooldownTicks() {
        return this.data.get(1);
    }

    public int getMode() {
        return this.data.get(2);
    }

    public int getIntervalIndex() {
        return this.data.get(3);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        switch (id) {
            case 0: // Toggle mode (auto <-> redstone)
                int newMode = this.data.get(2) == 0 ? 1 : 0;
                this.data.set(2, newMode);
                if (newMode == OmaraDeviceBlockEntity.MODE_REDSTONE) {
                    this.data.set(0, 0); // Ready immediately for redstone pulse
                } else {
                    int idx = this.data.get(3);
                    this.data.set(0, OmaraDeviceBlockEntity.INTERVAL_TICKS[idx]);
                }
                return true;
            case 1: // Cycle interval (60s <-> 5min)
                int newInterval = this.data.get(3) == 0 ? 1 : 0;
                this.data.set(3, newInterval);
                this.data.set(0, OmaraDeviceBlockEntity.INTERVAL_TICKS[newInterval]);
                return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index == 0) {
                // Move from device slot to player inventory
                if (!this.moveItemStackTo(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Only allow capsule items to shift-click into device slot
                if (isCapsuleItem(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate(
                (level, pos) -> player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                true
        );
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    private static boolean isCapsuleItem(ItemStack stack) {
        return stack.getItem() instanceof ScentItem si && si.getDefinition().isCapsule();
    }

    /**
     * Custom slot that only accepts scent capsule items.
     */
    private static class CapsuleSlot extends Slot {
        public CapsuleSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isCapsuleItem(stack);
        }
    }
}
