package com.ovrtechnology.tutorial;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.tutorial.spawn.TutorialSpawnManager;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.ButtonBlock;

import java.util.Optional;

/**
 * Handles the rain toggle button in the tutorial map.
 * When a player clicks the registered button, rain toggles on/off for 1 minute.
 */
public final class TutorialRainButtonHandler {

    private static boolean initialized = false;
    private static final long RAIN_DURATION_MS = 60000; // 1 minute
    private static final int RAIN_DURATION_TICKS = 1200; // 1 minute

    private TutorialRainButtonHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(serverPlayer.level() instanceof ServerLevel level)) return InteractionResult.PASS;
            if (!TutorialModule.isActive(level)) return InteractionResult.PASS;

            // Only handle button blocks
            if (!(level.getBlockState(pos).getBlock() instanceof ButtonBlock)) return InteractionResult.PASS;

            // Check if this is the registered rain button
            Optional<BlockPos> rainButtonOpt = TutorialSpawnManager.getRainButtonPos(level);
            if (rainButtonOpt.isEmpty() || !rainButtonOpt.get().equals(pos)) return InteractionResult.PASS;

            // Toggle rain
            if (level.isRaining()) {
                // Stop rain
                level.setWeatherParameters(6000, 0, false, false);
                TutorialDaylightHandler.allowRainFor(0); // cancel any rain allowance
                AromaAffect.LOGGER.info("Rain button: rain stopped by player {}", serverPlayer.getName().getString());
            } else {
                // Start rain
                TutorialDaylightHandler.allowRainFor(RAIN_DURATION_MS);
                level.setWeatherParameters(0, RAIN_DURATION_TICKS, true, false);
                AromaAffect.LOGGER.info("Rain button: rain started for 1 minute by player {}", serverPlayer.getName().getString());
            }

            return InteractionResult.PASS; // let the button animation play
        });

        AromaAffect.LOGGER.debug("Tutorial rain button handler initialized");
    }
}
