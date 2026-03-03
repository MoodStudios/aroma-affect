package com.ovrtechnology.tutorial;

import com.ovrtechnology.AromaAffect;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * In tutorial mode, ores drop ingots directly instead of raw ore.
 * This simplifies the tutorial flow so players don't need to smelt.
 */
public final class TutorialOreDropHandler {

    private static boolean initialized = false;

    private TutorialOreDropHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return EventResult.pass();
            }

            if (!TutorialModule.isActive(serverLevel)) {
                return EventResult.pass();
            }

            ItemStack drop = getOreDrop(state);
            if (drop == null) {
                return EventResult.pass();
            }

            // Remove the block without vanilla drops
            serverLevel.destroyBlock(pos, false);

            // Spawn the ingot
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            ItemEntity item = new ItemEntity(serverLevel, x, y, z, drop);
            item.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(item);

            // Give XP as if smelted
            if (player instanceof ServerPlayer) {
                player.giveExperiencePoints(1);
            }

            // Cancel the vanilla break (we already handled it)
            return EventResult.interruptFalse();
        });

        AromaAffect.LOGGER.debug("Tutorial ore drop handler initialized");
    }

    /**
     * Returns the ingot drop for an ore block, or null if not an ore we handle.
     */
    private static ItemStack getOreDrop(BlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return new ItemStack(Items.IRON_INGOT, 1);
        }
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            return new ItemStack(Items.GOLD_INGOT, 1);
        }

        return null;
    }
}
