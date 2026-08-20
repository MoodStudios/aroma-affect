package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Flipping a lever is a redstone activation.
 *
 * <p>Hooked on {@code useWithoutItem} rather than on {@code pull}, which would read as
 * the better target: vanilla's {@code useWithoutItem} calls {@code pull(state, level,
 * pos, null)} with a hardcoded null player, so a hook there can never tell who flipped
 * the switch. {@code ButtonBlock.press} does pass its player, hence the different
 * target in {@link ButtonPressServerMixin}.</p>
 */
@Mixin(LeverBlock.class)
public abstract class LeverUseServerMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), require = 0)
    private void aromaaffect$onLeverUsed(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) return;
        ServerEventBusHandler.onRedstoneActivated(player, level, pos, state);
    }
}
