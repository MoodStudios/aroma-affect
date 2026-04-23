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
import com.ovrtechnology.util.Texts;
import com.ovrtechnology.variant.CustomNoseItem;
import com.ovrtechnology.variant.CustomNoseRegistry;
import com.ovrtechnology.variant.NoseVariant;
import com.ovrtechnology.variant.NoseVariantRegistry;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.Comparator;
import java.util.Map;
import lombok.experimental.UtilityClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@UtilityClass
public final class ModCreativeTab {

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> AROMAAFFECT_TAB =
            TABS.register(
                    "aromaaffect_tab",
                    () ->
                            CreativeTabRegistry.create(
                                    builder -> {
                                        builder.icon(
                                                () -> {
                                                    for (RegistrySupplier<NoseItem> nose :
                                                            NoseRegistry.getAllNoses()) {
                                                        if (nose.isPresent()) {
                                                            return new ItemStack(nose.get());
                                                        }
                                                    }

                                                    return new ItemStack(Items.LEATHER_HELMET);
                                                });

                                        builder.title(Texts.tr("itemGroup.aromaaffect"));

                                        builder.displayItems(
                                                (parameters, output) -> {
                                                    NoseRegistry.getAllNosesAsList().stream()
                                                            .filter(
                                                                    nose ->
                                                                            nose.getDefinition()
                                                                                    .isEnabled())
                                                            .sorted(
                                                                    Comparator.comparing(
                                                                            NoseItem::getTier))
                                                            .forEach(
                                                                    nose ->
                                                                            output.accept(
                                                                                    new ItemStack(
                                                                                            nose)));

                                                    SnifferNoseRegistry.getAllSnifferNosesAsList()
                                                            .stream()
                                                            .filter(
                                                                    nose ->
                                                                            nose.getDefinition()
                                                                                    .isEnabled())
                                                            .sorted(
                                                                    Comparator.comparing(
                                                                            SnifferNoseItem
                                                                                    ::getTier))
                                                            .forEach(
                                                                    snifferNose ->
                                                                            output.accept(
                                                                                    new ItemStack(
                                                                                            snifferNose)));

                                                    ScentItemRegistry
                                                            .getScentItemsSortedByPriority()
                                                            .stream()
                                                            .filter(
                                                                    item ->
                                                                            item.getDefinition()
                                                                                    .isEnabled())
                                                            .forEach(
                                                                    scentItem ->
                                                                            output.accept(
                                                                                    new ItemStack(
                                                                                            scentItem)));

                                                    if (CustomNoseRegistry.getCUSTOM_NOSE()
                                                            .isPresent()) {
                                                        CustomNoseItem item =
                                                                CustomNoseRegistry.getCUSTOM_NOSE()
                                                                        .get();
                                                        for (Map.Entry<
                                                                        ResourceLocation,
                                                                        NoseVariant>
                                                                e :
                                                                        NoseVariantRegistry.all()
                                                                                .entrySet()) {
                                                            output.accept(
                                                                    CustomNoseItem.stackFor(
                                                                            item,
                                                                            e.getKey(),
                                                                            e.getValue()));
                                                        }
                                                    }

                                                    if (AromaGuideRegistry.getAROMA_GUIDE()
                                                            .isPresent()) {
                                                        output.accept(
                                                                new ItemStack(
                                                                        AromaGuideRegistry
                                                                                .getAROMA_GUIDE()
                                                                                .get()));
                                                    }

                                                    if (OmaraDeviceRegistry.OMARA_DEVICE_ITEM
                                                            .isPresent()) {
                                                        output.accept(
                                                                new ItemStack(
                                                                        OmaraDeviceRegistry
                                                                                .OMARA_DEVICE_ITEM
                                                                                .get()));
                                                    }

                                                    if (NoseSmithRegistry.getSPECIAL_ROSE()
                                                            .isPresent()) {
                                                        output.accept(
                                                                new ItemStack(
                                                                        NoseSmithRegistry
                                                                                .getSPECIAL_ROSE()
                                                                                .get()));
                                                    }

                                                    if (NoseSmithRegistry.getIRON_NOSE()
                                                            .isPresent()) {
                                                        output.accept(
                                                                new ItemStack(
                                                                        NoseSmithRegistry
                                                                                .getIRON_NOSE()
                                                                                .get()));
                                                    }

                                                    if (NoseSmithRegistry.getNOSE_SMITH_SPAWN_EGG()
                                                            .isPresent()) {
                                                        output.accept(
                                                                new ItemStack(
                                                                        NoseSmithRegistry
                                                                                .getNOSE_SMITH_SPAWN_EGG()
                                                                                .get()));
                                                    }
                                                });
                                    }));

    public static void init() {
        TABS.register();
        AromaAffect.LOGGER.info("Registered AromaAffect creative tab");
    }
}
