package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detects food being placed on a campfire (start of cooking) so the event-trigger system can fire
 * COOKING_STARTED (→ Smoky). Only fires when placement actually succeeded (a free slot + valid
 * recipe). Cross-platform (vanilla target); {@code require = 0} so a mapping mismatch degrades to
 * a no-op instead of a crash.
 */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireCookServerMixin {

    @Inject(method = "placeFood", at = @At("RETURN"), require = 0)
    private void aromaaffect$onPlaceFood(
            ServerLevel level,
            LivingEntity placer,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && placer instanceof ServerPlayer player) {
            ServerEventBusHandler.onCampfireCook(player);
        }
    }
}
