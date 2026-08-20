package com.ovrtechnology.trigger.client;

import com.ovrtechnology.category.CategoryDefinition;
import com.ovrtechnology.category.CategoryDefinitionLoader;
import com.ovrtechnology.util.SoundRef;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

public final class SoundTrigger {

    private static final SoundEvent DEFAULT_SOUND = SoundEvents.NOTE_BLOCK_CHIME.value();
    private static final float DEFAULT_VOLUME = 0.25f;
    private static final float DEFAULT_PITCH = 0.5f;

    public SoundTrigger() {
    }

    public static void playEventSound(Player player, String categoryID, SoundRef ref) {
        CategoryDefinition category = CategoryDefinitionLoader.getCategoryFromID(categoryID);

        // If category is not null, play the category sound over the soundRef
        if (category != null) {
            if (category.getSound() != null) {
                playSoundRef(player, category.getSound());
                return;
            }
        }

        if (ref != null) {
            playSoundRef(player, ref);
            return;
        }

        player.playSound(DEFAULT_SOUND, DEFAULT_VOLUME, DEFAULT_PITCH);
    }

    private static void playSoundRef(Player player, SoundRef soundRef) {
        var sound = BuiltInRegistries.SOUND_EVENT.get(Identifier.parse(soundRef.getId()));
        sound.ifPresent(sE -> player.playSound(sE.value(), soundRef.getVolume(), soundRef.getPitch()));
    }
}
