package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires PLAYER_DEATH (→ Machina) when a server player dies.
 *
 * <p>{@code ServerPlayer} overrides {@code die} with its own body (death message / telemetry) and
 * does not chain to {@code LivingEntity.die} on 26.2, so {@link LivingDeathServerMixin} — which
 * targets {@code LivingEntity.die} for the MOB_KILLED path — never observes a player's own death.
 * This mixin closes that gap by hooking the player's actual death entry point. It covers every
 * cause (mobs, falls, drowning, lava, {@code /kill}, …) because they all funnel through
 * {@code die}.</p>
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathServerMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void aromaaffect$onPlayerDie(DamageSource source, CallbackInfo ci) {
        ServerEventBusHandler.onLivingDeath((ServerPlayer) (Object) this, source);
    }
}
