package com.ovrtechnology.mixin;

import com.ovrtechnology.tracking.RespawnSyncHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRespawnMixin {

    //? if >=26 {
    @Inject(method = "setRespawnPosition", at = @At("TAIL"))
    private void aromaaffect$onSetRespawnPosition(ServerPlayer.RespawnConfig config, boolean showMessage, CallbackInfo ci) {
        RespawnSyncHandler.sync((ServerPlayer) (Object) this);
    }
    //?} else {
    /*@Inject(method = "setRespawnPosition", at = @At("TAIL"))
    private void aromaaffect$onSetRespawnPosition(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                                  net.minecraft.core.BlockPos pos, float angle, boolean forced,
                                                  boolean showMessage, CallbackInfo ci) {
        RespawnSyncHandler.sync((ServerPlayer) (Object) this);
    }
    *///?}
}
