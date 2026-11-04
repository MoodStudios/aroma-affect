package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Detects flint &amp; steel use for the event-trigger system. Cross-platform (vanilla target). */
@Mixin(FlintAndSteelItem.class)
public abstract class FlintUseServerMixin {

    @Inject(method = "useOn", at = @At("HEAD"), require = 0)
    private void aromaaffect$onFlintUse(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (ctx.getPlayer() instanceof ServerPlayer player) {
            ServerEventBusHandler.onFlintUsed(player);
        }
    }
}
