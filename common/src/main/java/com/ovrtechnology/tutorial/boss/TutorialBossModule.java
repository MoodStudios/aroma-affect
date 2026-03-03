package com.ovrtechnology.tutorial.boss;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.command.TutorialCommand;
import com.ovrtechnology.tutorial.boss.command.BossSubCommand;
import lombok.experimental.UtilityClass;

/**
 * Module for tutorial boss encounters.
 * <p>
 * Uses vanilla mobs (Blaze, etc.) with modified stats and custom drops.
 * Bosses spawn when player enters designated areas.
 */
@UtilityClass
public final class TutorialBossModule {

    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Register boss event handler (death drops, Oliver dialogues)
        TutorialBossHandler.init();

        // Register area-based spawning handler
        TutorialBossAreaHandler.init();

        // Register boss subcommand
        TutorialCommand.register(new BossSubCommand());

        AromaAffect.LOGGER.info("Tutorial Boss Module initialized");
    }
}
