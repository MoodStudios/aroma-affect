package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Detects a player applying bone meal that actually grew something. Dispensers have no player and are skipped. */
@Mixin(BoneMealItem.class)
public abstract class BoneMealUseServerMixin {
    @Inject(method = "useOn", at = @At("RETURN"), require = 0)
    private void aromaaffect$onBoneMealUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;
        if (!cir.getReturnValue().consumesAction()) return;

        ServerEventBusHandler.onBoneMealUsed(player);
    }
}
