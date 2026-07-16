package com.ovrtechnology.variant;

import java.util.function.Supplier;
import net.blay09.mods.balm.core.component.BalmDataComponentTypeRegistrar;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;

public final class ModDataComponents {

    private static Supplier<DataComponentType<Identifier>> noseVariant;

    private ModDataComponents() {}

    public static void register(BalmDataComponentTypeRegistrar registrar) {
        noseVariant = registrar
                .register("nose_variant", Identifier.CODEC, Identifier.STREAM_CODEC)
                .asSupplier();
    }

    public static DataComponentType<Identifier> noseVariant() {
        return noseVariant.get();
    }
}
