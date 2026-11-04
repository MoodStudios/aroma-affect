package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Detects firework rocket use for the event-trigger system. Cross-platform (vanilla target). */
@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketUseServerMixin {

    @Inject(method = "useOn", at = @At("HEAD"), require = 0)
    private void aromaaffect$onRocketUseOn(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (ctx.getPlayer() instanceof ServerPlayer player) {
            ServerEventBusHandler.fireSimpleEvent(player, ServerEventBusHandler.TT_FIREWORK_USED_ON);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), require = 0)
    private void aromaaffect$onRocketUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer sPlayer) {
            ServerEventBusHandler.fireSimpleEvent(sPlayer, ServerEventBusHandler.TT_FIREWORK_USED);
        }
    }
}
