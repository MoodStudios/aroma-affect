package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detects eating a bite of cake for the event-trigger system. Cross-platform (vanilla target).
 *
 * <p>Targets {@code CakeBlock.eat} rather than {@code useWithoutItem} because
 * {@code CandleCakeBlock} routes through the same method, so both plain and candle
 * cakes are covered by this single injection. Firing on RETURN and checking the
 * result means a right-click that did not actually consume a bite (player already
 * full) does not trigger a scent.</p>
 */
@Mixin(CakeBlock.class)
public abstract class CakeEatServerMixin {

    @Inject(method = "eat", at = @At("RETURN"))
    private static void aromaaffect$onEat(
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            Player player,
            CallbackInfoReturnable<InteractionResult> cir) {

        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) return;

        if (player instanceof ServerPlayer serverPlayer) {
            ServerEventBusHandler.onCakeEaten(serverPlayer);
        }
    }
}
