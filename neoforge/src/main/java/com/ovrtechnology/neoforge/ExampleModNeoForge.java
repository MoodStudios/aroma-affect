package com.ovrtechnology.neoforge;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.neoforge.accessory.CuriosIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(AromaAffect.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge(IEventBus modEventBus) {
        // Run our common setup.
        AromaAffect.init();

        // Curios is an optional accessory framework. Only wire its capability registration
        // when the mod is present; otherwise noses still work via the vanilla HEAD slot.
        if (ModList.get().isLoaded("curios")) {
            modEventBus.addListener(CuriosIntegration::register);
        }
    }
}
