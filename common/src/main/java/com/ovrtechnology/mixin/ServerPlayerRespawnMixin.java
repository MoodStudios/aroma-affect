package com.ovrtechnology.mixin;

import com.ovrtechnology.tracking.RespawnSyncHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRespawnMixin {

    @Inject(method = "setRespawnPosition", at = @At("TAIL"))
    private void aromaaffect$onSetRespawnPosition(ServerPlayer.RespawnConfig config, boolean showMessage, CallbackInfo ci) {
        RespawnSyncHandler.sync((ServerPlayer) (Object) this);
    }
}
