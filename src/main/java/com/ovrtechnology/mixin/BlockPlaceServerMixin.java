package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Detects a player placing any block, for {@code BLOCK_PLACED} events filtered by block id or tag. */
@Mixin(BlockItem.class)
public abstract class BlockPlaceServerMixin {

    @Inject(method = "place", at = @At("RETURN"), require = 0)
    private void aromaaffect$onBlockPlaced(BlockPlaceContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
        if (!cir.getReturnValue().consumesAction()) return;

        ServerEventBusHandler.onBlockPlaced(player, ((BlockItem) (Object) this).getBlock().defaultBlockState());
    }
}
