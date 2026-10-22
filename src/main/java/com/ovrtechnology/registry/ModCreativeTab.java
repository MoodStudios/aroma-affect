package com.ovrtechnology.registry;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.entity.nosesmith.NoseSmithRegistry;
import com.ovrtechnology.guide.AromaGuideRegistry;
import com.ovrtechnology.nose.NoseItem;
import com.ovrtechnology.nose.NoseRegistry;
import com.ovrtechnology.omara.OmaraDeviceRegistry;
import com.ovrtechnology.scentitem.ScentItemRegistry;
import com.ovrtechnology.sniffernose.SnifferNoseItem;
import com.ovrtechnology.sniffernose.SnifferNoseRegistry;
import lombok.experimental.UtilityClass;
import net.blay09.mods.balm.world.item.BalmCreativeModeTabRegistrar;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;

/**
 * Registers the creative mode tab for Aroma Affect items.
 *
 * <p>Invoked from {@link AromaAffect#initialize(net.blay09.mods.balm.core.BalmRegistrars)}
 * via {@code registrars.creativeModeTabs(ModCreativeTab::register)}.</p>
 */
@UtilityClass
public final class ModCreativeTab {

    public static void register(BalmCreativeModeTabRegistrar creativeModeTabs) {
        creativeModeTabs.register("aromaaffect_tab", builder -> builder
                .title(Component.translatable("itemGroup.aromaaffect"))
                .icon(() -> {
                    for (Holder<Item> holder : NoseRegistry.getAllNoses()) {
                        if (holder.isBound()) {
                            return new ItemStack(holder.value());
                        }
                    }
                    return new ItemStack(Items.LEATHER_HELMET);
                })
                .displayItems((parameters, output) -> {
                    NoseRegistry.getAllNosesAsList()
                            .stream().sorted(Comparator.comparing(NoseItem::getTier))
                            .forEach(nose -> output.accept(new ItemStack(nose)));

                    SnifferNoseRegistry.getAllSnifferNosesAsList()
                            .stream().sorted(Comparator.comparing(SnifferNoseItem::getTier))
                            .forEach(snifferNose -> output.accept(new ItemStack(snifferNose)));

                    ScentItemRegistry.getScentItemsSortedByPriority()
                            .forEach(scentItem -> output.accept(new ItemStack(scentItem)));

                    if (AromaGuideRegistry.getAromaGuide() != null
                            && AromaGuideRegistry.getAromaGuide().isBound()) {
                        output.accept(new ItemStack(AromaGuideRegistry.getAromaGuide().value()));
                    }

                    if (OmaraDeviceRegistry.OMARA_DEVICE_ITEM != null
                            && OmaraDeviceRegistry.OMARA_DEVICE_ITEM.isBound()) {
                        output.accept(new ItemStack(OmaraDeviceRegistry.OMARA_DEVICE_ITEM.value()));
                    }

                    if (NoseSmithRegistry.getSpecialRose() != null
                            && NoseSmithRegistry.getSpecialRose().isBound()) {
                        output.accept(new ItemStack(NoseSmithRegistry.getSpecialRose().value()));
                    }

                    if (NoseSmithRegistry.getIronNose() != null
                            && NoseSmithRegistry.getIronNose().isBound()) {
                        output.accept(new ItemStack(NoseSmithRegistry.getIronNose().value()));
                    }

                    if (NoseSmithRegistry.getNoseSmithSpawnEgg() != null
                            && NoseSmithRegistry.getNoseSmithSpawnEgg().isBound()) {
                        output.accept(new ItemStack(NoseSmithRegistry.getNoseSmithSpawnEgg().value()));
                    }
                }));
    }
}
