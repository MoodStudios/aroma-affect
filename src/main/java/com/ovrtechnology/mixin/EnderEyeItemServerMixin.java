package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemServerMixin {

    @Inject(
            method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),
            require = 0)
    private void aromaaffect$onEyeThrown(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerEventBusHandler.onEnderEyeThrown(serverPlayer);
        }
    }

    @Inject(
            method = "useOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;globalLevelEvent(ILnet/minecraft/core/BlockPos;I)V"),
            require = 0)
    private void aromaaffect$onEndPortalCreated(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        ServerEventBusHandler.onEndPortalCreated(ctx.getLevel(), ctx.getClickedPos(), ctx.getPlayer());
    }
}
