package com.ovrtechnology.registry;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.util.Ids;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> SNIFF =
            SOUNDS.register("sniff", () -> SoundEvent.createVariableRangeEvent(Ids.mod("sniff")));

    public static final RegistrySupplier<SoundEvent> OMARA_PUFF =
            SOUNDS.register(
                    "omara_puff", () -> SoundEvent.createVariableRangeEvent(Ids.mod("omara_puff")));

    public static void init() {
        SOUNDS.register();
        AromaAffect.LOGGER.debug("Registered custom sound events");
    }

    private ModSounds() {}
}
