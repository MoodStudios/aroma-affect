package com.ovrtechnology.neoforge;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.neoforge.accessory.CuriosIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(AromaAffect.MOD_ID)
public final class AromaAffectNeoForge {
    public AromaAffectNeoForge(IEventBus modEventBus) {

        AromaAffect.init();

        modEventBus.addListener(CuriosIntegration::register);
    }
}
