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

/** Detects planting of seed/crop items for the event-trigger system. Cross-platform (vanilla target). */
@Mixin(BlockItem.class)
public abstract class SeedPlantServerMixin {

    @Inject(method = "place", at = @At("RETURN"), require = 0)
    private void aromaaffect$onPlace(BlockPlaceContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (ctx.getPlayer() instanceof ServerPlayer player) {
            ServerEventBusHandler.onSeedPlanted(player, ctx.getItemInHand());
        }
    }
}
