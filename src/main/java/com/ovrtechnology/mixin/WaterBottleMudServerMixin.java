package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detects a player turning dirt into mud with a water bottle. Vanilla does the
 * conversion inside {@code PotionItem.useOn}; by the time it returns, the block is mud.
 */
@Mixin(PotionItem.class)
public abstract class WaterBottleMudServerMixin {

    @Inject(method = "useOn", at = @At("RETURN"), require = 0)
    private void aromaaffect$onUseOn(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
        if (!cir.getReturnValue().consumesAction()) return;
        if (!ctx.getLevel().getBlockState(ctx.getClickedPos()).is(Blocks.MUD)) return;

        ServerEventBusHandler.onBlockPlaced(player, Blocks.MUD.defaultBlockState());
    }
}
