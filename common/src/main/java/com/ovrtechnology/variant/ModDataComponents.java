package com.ovrtechnology.variant;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

public final class ModDataComponents {

    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<ResourceLocation>> NOSE_VARIANT =
            COMPONENTS.register(
                    "nose_variant",
                    () ->
                            DataComponentType.<ResourceLocation>builder()
                                    .persistent(ResourceLocation.CODEC)
                                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                                    .build());

    private ModDataComponents() {}

    public static void init() {
        COMPONENTS.register();
    }
}
