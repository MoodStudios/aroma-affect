package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OldMinecartBehavior.class)
public abstract class OldMinecartBehaviorServerMixin extends MinecartBehaviorServerMixin {

    protected OldMinecartBehaviorServerMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @Inject(method = "moveAlongTrack", at = @At("HEAD"))
    private void aromaaffect$onMoveAlongTrack(ServerLevel level, CallbackInfo ci) {
        onRideOverRail(ServerEventBusHandler.TT_MINECART_OVERLAP_POWERED_RAIL);
    }
}
