package com.ovrtechnology.neoforge;

import com.ovrtechnology.AromaAffect;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(AromaAffect.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge(ModContainer modContainer, IEventBus modEventBus) {
        // Curios capability and slot-enforcer registration is restored in phase 9
        // once the integration is ported against Curios 26.1.x.
        Balm.initializeMod(AromaAffect.MOD_ID, new NeoForgeLoadContext(modContainer, modEventBus), AromaAffect::initialize);
    }
}
