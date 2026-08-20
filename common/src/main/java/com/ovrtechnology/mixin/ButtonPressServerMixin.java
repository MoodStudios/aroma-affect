package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pressing a button is a redstone activation. Arrow-fired presses pass a null player
 * and are dropped downstream.
 */
@Mixin(ButtonBlock.class)
public abstract class ButtonPressServerMixin {

    @Inject(method = "press", at = @At("HEAD"), require = 0)
    private void aromaaffect$onButtonPressed(
            BlockState state, Level level, BlockPos pos, Player player, CallbackInfo ci) {
        ServerEventBusHandler.onRedstoneActivated(player, level, pos, state);
    }
}
