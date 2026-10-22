package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShoulderRidingEntity.class)
public class ShoulderRidingServerMixin {
    @Inject(method = "setEntityOnShoulder", at = @At("RETURN"))
    private void aromaaffect$onSitOnShoulder(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ServerEventBusHandler.fireSimpleEvent(player, ServerEventBusHandler.TT_ENTITY_RIDE_SHOULDER);
        }
    }
}
