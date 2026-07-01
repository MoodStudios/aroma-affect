package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.advancements.criterion.UsedTotemTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UsedTotemTrigger.class)
public abstract class TotemTriggerServerMixin {

    @Inject(method = "trigger", at = @At("HEAD"), require = 0)
    private void aromaaffect$onTrigger(ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        ServerEventBusHandler.onTotemUse(player, stack);
    }
}
