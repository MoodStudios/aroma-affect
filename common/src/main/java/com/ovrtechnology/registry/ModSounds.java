package com.ovrtechnology.registry;

import com.ovrtechnology.AromaAffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

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

    // ─────────────────────────────────────────────────────────────────────────────
    // Dialogue ID to Sound Mapping (must be declared before dialogue registrations)
    // ─────────────────────────────────────────────────────────────────────────────

    private static final Map<String, RegistrySupplier<SoundEvent>> DIALOGUE_SOUNDS = new HashMap<>();

    private static RegistrySupplier<SoundEvent> registerDialogue(String name) {
        RegistrySupplier<SoundEvent> supplier = SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, name)));
        // Map dialogueId (without "dialogue_" prefix) to the sound
        String dialogueId = name.replace("dialogue_", "");
        DIALOGUE_SOUNDS.put(dialogueId, supplier);
        return supplier;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Dialogue Voice Lines
    // ─────────────────────────────────────────────────────────────────────────────

    public static final RegistrySupplier<SoundEvent> DIALOGUE_OLIVER_GREETING = registerDialogue("dialogue_oliver_greeting");
    public static final RegistrySupplier<SoundEvent> DIALOGUE_OLIVER_MINE_OPEN = registerDialogue("dialogue_oliver_mine_open");
    public static final RegistrySupplier<SoundEvent> DIALOGUE_OLIVER_FIND_IRON = registerDialogue("dialogue_oliver_find_iron");
    public static final RegistrySupplier<SoundEvent> DIALOGUE_OLIVER_NEED_IRON = registerDialogue("dialogue_oliver_need_iron");
    public static final RegistrySupplier<SoundEvent> DIALOGUE_OLIVER_PATH_OPENED = registerDialogue("dialogue_oliver_path_opened");
    public static final RegistrySupplier<SoundEvent> DIALOGUE_OLIVER_NEED_GOLD = registerDialogue("dialogue_oliver_need_gold");
    public static final RegistrySupplier<SoundEvent> DIALOGUE_OLIVER_EQUIP_PROSPECTOR = registerDialogue("dialogue_oliver_equip_prospector");
    public static final RegistrySupplier<SoundEvent> DIALOGUE_OLIVER_DRAGON_QUEST = registerDialogue("dialogue_oliver_dragon_quest");

    public static final RegistrySupplier<SoundEvent> DIALOGUE_BOSS_BLAZE_ENTER = registerDialogue("dialogue_boss_blaze_enter");
    public static final RegistrySupplier<SoundEvent> DIALOGUE_BOSS_BLAZE_KILLED = registerDialogue("dialogue_boss_blaze_killed");
    public static final RegistrySupplier<SoundEvent> DIALOGUE_BOSS_DRAGON_ENTER = registerDialogue("dialogue_boss_dragon_enter");
    public static final RegistrySupplier<SoundEvent> DIALOGUE_BOSS_DRAGON_KILLED = registerDialogue("dialogue_boss_dragon_killed");

    public static final RegistrySupplier<SoundEvent> DIALOGUE_DREAM_END_WAKEUP = registerDialogue("dialogue_dream_end_wakeup");

    /**
     * Gets the SoundEvent for a dialogue ID.
     * The dialogueId should match the file name without extension (e.g., "oliver_greeting").
     *
     * @param dialogueId The dialogue identifier
     * @return The SoundEvent, or null if no sound exists for this dialogue
     */
    @Nullable
    public static SoundEvent getDialogueSound(String dialogueId) {
        RegistrySupplier<SoundEvent> supplier = DIALOGUE_SOUNDS.get(dialogueId);
        if (supplier != null && supplier.isPresent()) {
            return supplier.get();
        }
        return null;
    }

    /**
     * Checks if a dialogue has an associated voice sound.
     */
    public static boolean hasDialogueSound(String dialogueId) {
        return DIALOGUE_SOUNDS.containsKey(dialogueId);
    }

    public static void init() {
        SOUNDS.register();
        AromaAffect.LOGGER.debug("Registered custom sound events");
    }

    private ModSounds() {}
}
