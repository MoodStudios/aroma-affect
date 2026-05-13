package com.ovrtechnology.neoforge;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.neoforge.accessory.CuriosIntegration;
import com.ovrtechnology.neoforge.accessory.NoseSlotEnforcer;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(AromaAffect.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge(ModContainer modContainer, IEventBus modEventBus) {
        Balm.initializeMod(AromaAffect.MOD_ID, new NeoForgeLoadContext(modContainer, modEventBus), AromaAffect::initialize);

        // Curios is an optional accessory framework. Only wire its capability
        // registration when the mod is present; otherwise noses still work via
        // the vanilla HEAD slot through NoseAccessoryImpl's fallback path.
        if (Balm.platform().isModLoaded("curios")) {
            modEventBus.addListener(CuriosIntegration::register);
            NoseSlotEnforcer.register();
        }
    }
}
