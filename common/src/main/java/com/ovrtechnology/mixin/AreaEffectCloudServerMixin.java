package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudServerMixin {

    @Inject(method = "serverTick", at = @At("HEAD"), require = 0)
    private void aromaaffect$onServerTick(ServerLevel level, CallbackInfo ci) {
        ServerEventBusHandler.onEffectCloudTick((AreaEffectCloud) (Object) this, level);
    }
}
