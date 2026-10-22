package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekServerMixin {

    @Inject(method = "shriek", at = @At("HEAD"), require = 0)
    private void aromaaffect$onShriek(ServerLevel level, Entity sourceEntity, CallbackInfo ci) {
        if (sourceEntity instanceof ServerPlayer player) {
            ServerEventBusHandler.onSculkShriek(player);
        }
    }
}
