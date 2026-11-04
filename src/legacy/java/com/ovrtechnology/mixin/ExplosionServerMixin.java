package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1.21.1 explodes through Explosion itself (ServerExplosion arrived in 1.21.2) and also runs it client-side
@Mixin(Explosion.class)
public abstract class ExplosionServerMixin {

    @Shadow
    @Final
    private Level level;

    @Inject(method = "explode", at = @At("HEAD"), require = 0)
    private void aromaaffect$onExplode(CallbackInfo ci) {
        if (!level.isClientSide()) {
            ServerEventBusHandler.onExplosion((Explosion) (Object) this, level);
        }
    }
}
