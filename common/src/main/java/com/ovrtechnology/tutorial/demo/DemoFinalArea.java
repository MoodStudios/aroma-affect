package com.ovrtechnology.tutorial.demo;

import com.ovrtechnology.network.TutorialScentZoneNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The free area the player wakes up in after the dragon, where the 16-scent hunt happens.
 */
public final class DemoFinalArea {

    private static final int MIN_X = -45;
    private static final int MAX_X = 90;
    private static final int MIN_Z = -305;
    private static final int MAX_Z = -195;

    private static final long MINE_SCENT_COOLDOWN_MS = 3000;
    private static final Map<UUID, Long> lastMineScent = new ConcurrentHashMap<>();

    private DemoFinalArea() {
    }

    public static boolean contains(Level level, BlockPos pos) {
        return level.dimension() == Level.OVERWORLD
                && pos.getX() >= MIN_X && pos.getX() <= MAX_X
                && pos.getZ() >= MIN_Z && pos.getZ() <= MAX_Z;
    }

    /** Ore blocks here can be broken, so mining is one of the ways to find a scent. */
    public static boolean isMinableOre(ServerLevel level, BlockPos pos, BlockState state) {
        return contains(level, pos) && isOre(state);
    }

    private static boolean isOre(BlockState state) {
        return state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES) || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES);
    }

    /**
     * The player broke an ore here. The ore stays where it is, since the area floats over the void and a
     * missing block is a hole to fall through; the player gets the break effect and the ore's scent.
     */
    public static void mineOre(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        level.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));

        long now = System.currentTimeMillis();
        Long last = lastMineScent.get(player.getUUID());
        if (last != null && now - last < MINE_SCENT_COOLDOWN_MS) return;
        lastMineScent.put(player.getUUID(), now);

        String scent = state.is(BlockTags.REDSTONE_ORES) ? "Machina" : "Terra Silva";
        TutorialScentZoneNetworking.sendScentTrigger(player, scent, 1.0, "final_area_ore");
    }

    /** Forgets the previous world's players. */
    public static void reset() {
        lastMineScent.clear();
    }

    /** Called after the inventory is cleared at the end of the dream. */
    public static void giveTools(ServerPlayer player) {
        player.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
    }
}
