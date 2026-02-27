package com.ovrtechnology.tutorial;

import com.ovrtechnology.AromaAffect;
import lombok.experimental.UtilityClass;
import net.minecraft.world.level.GameRules;

/**
 * Registers custom GameRules for the tutorial system.
 * <p>
 * The {@code isOvrTutorial} GameRule acts as the master switch for the tutorial module.
 * Map creators set this to {@code true} in their tutorial maps, and it gets saved
 * in the world's {@code level.dat} file.
 * <p>
 * Usage for map creators:
 * <pre>
 *   /gamerule isOvrTutorial true
 * </pre>
 * <p>
 * <b>Important:</b> GameRules must be registered statically during class loading.
 * The {@link #init()} method just ensures this class is loaded early enough.
 */
@UtilityClass
public final class TutorialGameRules {

    /**
     * GameRule key for enabling tutorial mode.
     * <p>
     * When {@code true}, tutorial commands become available and tutorial
     * features are activated. Default is {@code false}.
     * <p>
     * This is registered statically to ensure it's available before any world loads.
     */
    public static final GameRules.Key<GameRules.BooleanValue> IS_OVR_TUTORIAL =
            GameRules.register(
                    "isOvrTutorial",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false)
            );

    /**
     * Ensures this class is loaded and the GameRule is registered.
     * <p>
     * Called during module initialization. The actual registration happens
     * in the static initializer above when the class is first loaded.
     */
    public static void init() {
        // This method just ensures the class is loaded, triggering the static initializer.
        // The GameRule is already registered by this point.
        AromaAffect.LOGGER.debug("GameRule isOvrTutorial initialized (key: {})", IS_OVR_TUTORIAL);
    }
}
