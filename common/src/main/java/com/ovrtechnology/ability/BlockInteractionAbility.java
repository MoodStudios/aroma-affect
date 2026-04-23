package com.ovrtechnology.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

public interface BlockInteractionAbility extends Ability {

    boolean isValidTarget(Block block);

    boolean onInteract(ServerPlayer player, BlockPos pos);

    void onCancel(ServerPlayer player);

    boolean onTick(ServerPlayer player, BlockPos pos);

    boolean isActive(ServerPlayer player);

    float getProgress(ServerPlayer player);

    long getRemainingCooldown(ServerPlayer player);
}
