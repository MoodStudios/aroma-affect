package com.ovrtechnology.neoforge;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.neoforge.accessory.CuriosIntegration;
import com.ovrtechnology.neoforge.accessory.NoseSlotEnforcer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(AromaAffect.MOD_ID)
public final class AromaAffectNeoForge {
    public AromaAffectNeoForge(IEventBus modEventBus) {

        AromaAffect.init();

        // Curios is an optional accessory framework. Only wire its capability registration
        // when the mod is present; otherwise noses still work via the vanilla HEAD slot.
        if (ModList.get().isLoaded("curios")) {
            modEventBus.addListener(CuriosIntegration::register);
            NoseSlotEnforcer.register();
        }
    }
}
