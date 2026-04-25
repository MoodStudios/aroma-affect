package com.ovrtechnology.neoforge;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.neoforge.accessory.CuriosIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(AromaAffect.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge(IEventBus modEventBus) {
        // Run our common setup.
        AromaAffect.init();

        // Register Curios capability for nose items.
        modEventBus.addListener(CuriosIntegration::register);
    }
}
