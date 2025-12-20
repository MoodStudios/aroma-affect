package com.ovrtechnology.registry;

import com.ovrtechnology.AromaCraft;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import lombok.experimental.UtilityClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.Map;

/**
 * Registers the creative mode tab for AromaCraft items.
 */
@UtilityClass
public final class ModCreativeTab {
    
    /**
     * Deferred register for creative tabs
     */
    private static final DeferredRegister<CreativeModeTab> TABS = 
            DeferredRegister.create(AromaCraft.MOD_ID, Registries.CREATIVE_MODE_TAB);
    
    /**
     * The main AromaCraft creative tab
     */
    public static final RegistrySupplier<CreativeModeTab> AROMACRAFT_TAB = TABS.register(
            "aromacraft_tab",
            () -> CreativeTabRegistry.create(builder -> {
                // Set the tab icon
                builder.icon(() -> {
                    // Get the first registered nose as the icon
                    for (RegistrySupplier<NoseItem> nose : NoseRegistry.getAllNoses()) {
                        if (nose.isPresent()) {
                            return new ItemStack(nose.get());
                        }
                    }
                    // Fallback if no noses registered
                    return new ItemStack(Items.LEATHER_HELMET);
                });

                // Set the tab title
                builder.title(Component.translatable("itemGroup.aromacraft"));

                // Add all items from our mod to this tab
                builder.displayItems((parameters, output) -> {
                    // Add all items registered under our mod ID
                   NoseRegistry.getAllNosesAsList()
                           .stream().sorted(Comparator.comparing(NoseItem::getTier))
                           .forEach(nose -> output.accept(new ItemStack(nose)));
                });
            })
    );
    
    /**
     * Initialize the creative tab registration.
     * Must be called during mod initialization.
     */
    public static void init() {
        TABS.register();
        AromaCraft.LOGGER.info("Registered AromaCraft creative tab");
    }
}
