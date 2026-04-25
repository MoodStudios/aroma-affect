package com.ovrtechnology.neoforge.accessory;

import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;

public final class CuriosIntegration {

    private CuriosIntegration() {}

    public static void register(RegisterCapabilitiesEvent event) {
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getAllNoses()) {
            event.registerItem(
                    CuriosCapability.ITEM, CuriosIntegration::createCurio, supplier.get());
        }
        for (RegistrySupplier<NoseItem> supplier : NoseRegistry.getLegacyItems()) {
            event.registerItem(
                    CuriosCapability.ITEM, CuriosIntegration::createCurio, supplier.get());
        }
    }

    private static ICurio createCurio(ItemStack stack, Void context) {
        return new ICurio() {
            @Override
            public ItemStack getStack() {
                return stack;
            }
        };
    }
}
