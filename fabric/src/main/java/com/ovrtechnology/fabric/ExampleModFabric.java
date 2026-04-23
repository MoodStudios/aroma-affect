package com.ovrtechnology.fabric;

import com.ovrtechnology.AromaAffect;
import net.fabricmc.api.ModInitializer;

public final class ExampleModFabric implements ModInitializer {
    @Override
    public void onInitialize() {

        AromaAffect.init();
    }
}
