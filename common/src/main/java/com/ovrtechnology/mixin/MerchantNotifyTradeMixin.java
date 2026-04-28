package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public abstract class MerchantNotifyTradeMixin {

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void aromaaffect$onTrade(MerchantOffer offer, CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        if (self.level().isClientSide()) return;
        Player trader = self.getTradingPlayer();
        if (!(trader instanceof ServerPlayer serverPlayer)) return;
        ServerEventBusHandler.fireSimpleEvent(
                serverPlayer, ServerEventBusHandler.TT_TRADE_COMPLETED);
    }
}
