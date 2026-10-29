package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1.21.1 has no MinecartBehavior split; rail movement lives in AbstractMinecart
@Mixin(AbstractMinecart.class)
public abstract class MinecartBehaviorServerMixin {

    @Inject(method = "moveAlongTrack", at = @At("HEAD"))
    private void aromaaffect$onMoveAlongTrack(BlockPos pos, BlockState state, CallbackInfo ci) {
        AbstractMinecart minecart = (AbstractMinecart) (Object) this;
        if (minecart.level().isClientSide() || !state.is(Blocks.POWERED_RAIL)) {
            return;
        }
        for (Entity entity : minecart.getPassengers()) {
            if (entity instanceof ServerPlayer player) {
                ServerEventBusHandler.fireSimpleEvent(player, ServerEventBusHandler.TT_MINECART_OVERLAP_POWERED_RAIL);
            }
        }
    }
}
