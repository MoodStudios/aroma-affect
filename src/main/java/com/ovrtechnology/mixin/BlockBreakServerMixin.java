package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Detects server-side block breaks for the event-trigger system (Balm has no free event API). */
@Mixin(ServerPlayerGameMode.class)
public abstract class BlockBreakServerMixin {

    @Shadow protected ServerLevel level;
    @Shadow protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void aromaaffect$onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level != null && player != null) {
            ServerEventBusHandler.onBlockBroken(player, level.getBlockState(pos));
        }
    }
}
