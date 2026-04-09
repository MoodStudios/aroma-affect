package com.ovrtechnology.tutorial;

import com.ovrtechnology.AromaAffect;
import com.ovrtechnology.network.TutorialScentZoneNetworking;
import com.ovrtechnology.tutorial.spawn.TutorialSpawnManager;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;

import java.util.Optional;

/**
 * When the player clicks the fountain button, replaces the target block
 * with water and triggers Marine scent.
 */
public final class TutorialFountainHandler {

    private static boolean initialized = false;
    private static boolean fountainActivated = false;

    private TutorialFountainHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(serverPlayer.level() instanceof ServerLevel level)) return InteractionResult.PASS;
            if (!TutorialModule.isActive(level)) return InteractionResult.PASS;
            if (!(level.getBlockState(pos).getBlock() instanceof ButtonBlock)) return InteractionResult.PASS;

            Optional<BlockPos> btnOpt = TutorialSpawnManager.getFountainButtonPos(level);
            if (btnOpt.isEmpty() || !btnOpt.get().equals(pos)) return InteractionResult.PASS;

            // Don't trigger again if already activated
            if (fountainActivated) return InteractionResult.PASS;

            Optional<BlockPos> blockOpt = TutorialSpawnManager.getFountainBlockPos(level);
            if (blockOpt.isEmpty()) return InteractionResult.PASS;

            BlockPos waterPos = blockOpt.get();

            // Replace block with water
            level.setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
            fountainActivated = true;

            // Water sound
            level.playSound(null, waterPos.getX(), waterPos.getY(), waterPos.getZ(),
                    SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 1.0f, 1.0f);

            // Trigger Marine scent
            TutorialScentZoneNetworking.sendScentTrigger(serverPlayer, "Marine", 1.0, "fountain");
            AromaAffect.LOGGER.info("Fountain activated by player {} at {}",
                    serverPlayer.getName().getString(), waterPos);

            return InteractionResult.PASS;
        });

        AromaAffect.LOGGER.debug("Tutorial fountain handler initialized");
    }

    /**
     * Resets the fountain state (for tutorial restart).
     */
    public static void reset() {
        fountainActivated = false;
    }
}
