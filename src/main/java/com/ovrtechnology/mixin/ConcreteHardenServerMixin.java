package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detects concrete powder turning into concrete. It happens three ways: powder falls
 * into water, powder is placed straight into water, or water flows next to powder.
 */
@Mixin(ConcretePowderBlock.class)
public abstract class ConcreteHardenServerMixin {

    @Shadow
    @Final
    private Block concrete;

    @Inject(method = "onLand", at = @At("RETURN"), require = 0)
    private void aromaaffect$onLand(Level level, BlockPos pos, BlockState state, BlockState replaceableState, FallingBlockEntity entity, CallbackInfo ci) {
        if (level.getBlockState(pos).is(concrete)) {
            ServerEventBusHandler.onConcreteHardened(level, pos);
        }
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), require = 0)
    private void aromaaffect$onPlacement(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {
        BlockState result = cir.getReturnValue();
        if (result != null && result.is(concrete)) {
            ServerEventBusHandler.onConcreteHardened(ctx.getLevel(), ctx.getClickedPos());
        }
    }

    @Inject(method = "updateShape", at = @At("RETURN"), require = 0)
    private void aromaaffect$onUpdateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
        if (!state.is(concrete) && cir.getReturnValue().is(concrete) && level instanceof Level realLevel) {
            ServerEventBusHandler.onConcreteHardened(realLevel, pos);
        }
    }
}
