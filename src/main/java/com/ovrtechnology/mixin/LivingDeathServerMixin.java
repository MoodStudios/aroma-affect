package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Detects mob deaths killed by a player for the event-trigger system. */
@Mixin(LivingEntity.class)
public abstract class LivingDeathServerMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void aromaaffect$onDie(DamageSource source, CallbackInfo ci) {
        ServerEventBusHandler.onLivingDeath((LivingEntity) (Object) this, source);
    }
}
