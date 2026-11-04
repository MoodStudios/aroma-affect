package com.ovrtechnology.fabric;

import com.ovrtechnology.AromaAffect;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext;
import net.fabricmc.api.ModInitializer;

public final class ExampleModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Balm.initializeMod(AromaAffect.MOD_ID, FabricLoadContext.INSTANCE, AromaAffect::initialize);
    }
}
