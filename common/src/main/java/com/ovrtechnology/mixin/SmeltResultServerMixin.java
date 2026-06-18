package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Detects smelted results being taken from a furnace for the event-trigger system. */
@Mixin(FurnaceResultSlot.class)
public abstract class SmeltResultServerMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void aromaaffect$onSmelt(Player player, ItemStack stack, CallbackInfo ci) {
        ServerEventBusHandler.onItemSmelted(player, stack);
    }
}
