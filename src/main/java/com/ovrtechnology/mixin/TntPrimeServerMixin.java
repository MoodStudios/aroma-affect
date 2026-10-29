package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects TNT being lit. Every way of lighting TNT (flint and steel, fire charges,
 * redstone, flaming arrows, dispensers, other explosions) creates a primed TNT entity
 * through this constructor. Entities loaded from disk use a different constructor.
 */
@Mixin(PrimedTnt.class)
public abstract class TntPrimeServerMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/entity/LivingEntity;)V", at = @At("RETURN"), require = 0)
    private void aromaaffect$onPrimed(Level level, double x, double y, double z, LivingEntity owner, CallbackInfo ci) {
        ServerEventBusHandler.onTntIgnited((PrimedTnt) (Object) this);
    }
}
