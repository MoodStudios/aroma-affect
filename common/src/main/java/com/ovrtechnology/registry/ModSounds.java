package com.ovrtechnology.registry;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Registry for custom sound events used by Aroma Affect.
 */
public final class ModSounds {

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(AromaAffect.MOD_ID, Registries.SOUND_EVENT);

    /**
     * Sniff sound played when a tracking target is selected.
     * Minecraft automatically picks a random variant from sounds.json.
     */
    public static final RegistrySupplier<SoundEvent> SNIFF = SOUNDS.register("sniff",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "sniff")));

    public static final RegistrySupplier<SoundEvent> OMARA_PUFF = SOUNDS.register("omara_puff",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "omara_puff")));

    public static void init() {
        SOUNDS.register();
        AromaAffect.LOGGER.debug("Registered custom sound events");
    }

    private ModSounds() {}
}
