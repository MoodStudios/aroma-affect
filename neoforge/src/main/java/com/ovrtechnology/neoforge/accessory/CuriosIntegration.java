package com.ovrtechnology.neoforge.accessory;

import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public final class CuriosIntegration {

    private CuriosIntegration() {}

    public static void register(RegisterCapabilitiesEvent event) {
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getAllNoses()) {
            event.registerItem(CuriosCapability.ITEM, CuriosIntegration::createCurio, supplier.get());
        }
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getLegacyItems()) {
            event.registerItem(CuriosCapability.ITEM, CuriosIntegration::createCurio, supplier.get());
        }
    }

    private static ICurio createCurio(ItemStack stack, Void context) {
        return new ICurio() {
            @Override
            public ItemStack getStack() {
                return stack;
            }

            /**
             * Mirror of {@link NoseSlotEnforcer} for the opposite direction:
             * when a nose is equipped to the Curios face slot and the player
             * is already wearing another nose in HEAD, push the HEAD one back
             * to the inventory so we never have two noses on the same player.
             */
            @Override
            public void onEquip(SlotContext slotContext, ItemStack prevStack) {
                if (!(slotContext.entity() instanceof Player player)) return;
                if (player.level().isClientSide()) return;

                ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
                if (!(head.getItem() instanceof NoseItem)) return;

                ItemStack displaced = head.copy();
                player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                if (!player.getInventory().add(displaced)) {
                    player.drop(displaced, false);
                }
            }
        };
    }
}
