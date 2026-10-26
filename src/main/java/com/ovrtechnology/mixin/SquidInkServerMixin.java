package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.world.entity.animal.squid.Squid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Detects a squid (or glow squid) releasing ink after being attacked. Only runs server-side, from {@code hurtServer}. */
@Mixin(Squid.class)
public abstract class SquidInkServerMixin {
    @Inject(method = "spawnInk", at = @At("HEAD"), require = 0)
    private void aromaaffect$onSpawnInk(CallbackInfo ci) {
        ServerEventBusHandler.onSquidInk((Squid) (Object) this);
    }
}
