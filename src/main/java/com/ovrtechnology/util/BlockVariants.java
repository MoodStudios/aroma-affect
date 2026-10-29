package com.ovrtechnology.util;

import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Matches a tracked block against the world, including blocks that the game
 * stores under more than one ID.
 *
 * <p>Climbing plants such as weeping vines use one block for the tip and a
 * separate block for the rest of the stem. A scent definition names the tip,
 * which is also the item the player knows, so the stem has to count as well or
 * a long vine would only be detected at its last block.</p>
 */
public final class BlockVariants {

    private static final Map<Block, Block> STEMS = Map.of(
            Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT,
            Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT,
            Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT,
            Blocks.KELP, Blocks.KELP_PLANT);

    private BlockVariants() {}

    public static boolean matches(BlockState state, Block target) {
        if (state.is(target)) return true;
        Block stem = STEMS.get(target);
        return stem != null && state.is(stem);
    }
}
