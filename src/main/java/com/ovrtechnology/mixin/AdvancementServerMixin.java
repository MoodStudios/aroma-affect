package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detects advancement completion for the event-trigger system (Balm has no
 * free advancement event). Fires only when an award newly completes the
 * advancement (all criteria done).
 */
@Mixin(PlayerAdvancements.class)
public abstract class AdvancementServerMixin {

    @Shadow private ServerPlayer player;

    @Shadow public abstract AdvancementProgress getOrStartProgress(AdvancementHolder holder);

    @Inject(method = "award", at = @At("RETURN"), require = 0)
    private void aromaaffect$onAward(
            AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()
                && player != null
                && getOrStartProgress(advancement).isDone()) {
            ServerEventBusHandler.onAdvancement(player, advancement);
        }
    }
}
