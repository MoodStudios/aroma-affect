package com.ovrtechnology.registry;

import com.ovrtechnology.util.Texts;
import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import com.ovrtechnology.guide.AromaGuideRegistry;
import com.ovrtechnology.omara.OmaraDeviceRegistry;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.scentitem.ScentItemRegistry;
import com.ovrtechnology.sniffernose.SnifferNoseItem;
import com.ovrtechnology.sniffernose.SnifferNoseRegistry;
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

/**
 * Registers the creative mode tab for Aroma Affect items.
 */
@UtilityClass
public final class ModCreativeTab {
    
    /**
     * Deferred register for creative tabs
     */
    private static final DeferredRegister<CreativeModeTab> TABS = 
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.CREATIVE_MODE_TAB);
    
    /**
     * The main Aroma Affect creative tab
     */
    public static final RegistrySupplier<CreativeModeTab> AROMAAFFECT_TAB = TABS.register(
            "aromaaffect_tab",
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
                builder.title(Texts.tr("itemGroup.aromaaffect"));

                // Add all items from our mod to this tab
                builder.displayItems((parameters, output) -> {
                    // Add all player-equippable nose items sorted by tier
                    NoseRegistry.getAllNosesAsList()
                            .stream().sorted(Comparator.comparing(NoseItem::getTier))
                            .forEach(nose -> output.accept(new ItemStack(nose)));
                    
                    // Add all sniffer nose items sorted by tier
                    SnifferNoseRegistry.getAllSnifferNosesAsList()
                            .stream().sorted(Comparator.comparing(SnifferNoseItem::getTier))
                            .forEach(snifferNose -> output.accept(new ItemStack(snifferNose)));
                    
                    // Add all scent items sorted by priority
                    ScentItemRegistry.getScentItemsSortedByPriority()
                            .forEach(scentItem -> output.accept(new ItemStack(scentItem)));
                    
                    // Add Aroma Guide
                    if (AromaGuideRegistry.getAROMA_GUIDE().isPresent()) {
                        output.accept(new ItemStack(AromaGuideRegistry.getAROMA_GUIDE().get()));
                    }

                    // Add Omara Device
                    if (OmaraDeviceRegistry.OMARA_DEVICE_ITEM.isPresent()) {
                        output.accept(new ItemStack(OmaraDeviceRegistry.OMARA_DEVICE_ITEM.get()));
                    }

                    // Add Special Rose
                    if (NoseSmithRegistry.getSPECIAL_ROSE().isPresent()) {
                        output.accept(new ItemStack(NoseSmithRegistry.getSPECIAL_ROSE().get()));
                    }

                    // Add Iron Nose
                    if (NoseSmithRegistry.getIRON_NOSE().isPresent()) {
                        output.accept(new ItemStack(NoseSmithRegistry.getIRON_NOSE().get()));
                    }

                    // Add spawn eggs
                    if (NoseSmithRegistry.getNOSE_SMITH_SPAWN_EGG().isPresent()) {
                        output.accept(new ItemStack(NoseSmithRegistry.getNOSE_SMITH_SPAWN_EGG().get()));
                    }
                });
            })
    );
    
    /**
     * Initialize the creative tab registration.
     * Must be called during mod initialization.
     */
    public static void init() {
        TABS.register();
        AromaAffect.LOGGER.info("Registered AromaAffect creative tab");
    }
}
