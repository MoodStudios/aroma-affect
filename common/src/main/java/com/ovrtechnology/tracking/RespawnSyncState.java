package com.ovrtechnology.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;

import java.util.Optional;

public final class RespawnSyncState {

    private static boolean present = false;
    private static Identifier dimension = null;
    private static BlockPos pos = null;
    private static Identifier blockId = null;

    private RespawnSyncState() {
    }

    public static void update(String dimensionId, BlockPos position, String block) {
        present = true;
        dimension = Identifier.parse(dimensionId);
        pos = position;
        blockId = Identifier.parse(block);
    }

    public static void clear() {
        present = false;
        dimension = null;
        pos = null;
        blockId = null;
    }

    public static boolean hasRespawnPoint() {
        return present && pos != null && blockId != null;
    }

    public static Identifier getDimension() {
        return dimension;
    }

    public static BlockPos getPos() {
        return pos;
    }

    public static Identifier getBlockId() {
        return blockId;
    }

    public static boolean isInDimension(Level level) {
        return hasRespawnPoint() && level != null && level.dimension().identifier().equals(dimension);
    }

    public static ItemStack getIcon() {
        if (blockId == null) return new ItemStack(Items.BED.pick(DyeColor.RED));
        return BuiltInRegistries.ITEM.getOptional(blockId)
                .map(ItemStack::new)
                .orElseGet(() -> new ItemStack(Items.BED.pick(DyeColor.RED)));
    }

    public static Optional<DyeColor> getBedColor() {
        return bedColorOf(blockId);
    }

    public static Optional<DyeColor> bedColorOf(Identifier id) {
        if (id == null) return Optional.empty();
        return BuiltInRegistries.BLOCK.getOptional(id)
                .filter(block -> block instanceof BedBlock)
                .map(block -> ((BedBlock) block).getColor());
    }

    public static boolean isRespawnBlock(Identifier id) {
        if (id == null) return false;
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        return block.isPresent() && (block.get() instanceof BedBlock || block.get() instanceof RespawnAnchorBlock);
    }
}
