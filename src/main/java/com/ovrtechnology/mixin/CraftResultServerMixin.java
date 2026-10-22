package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Detects crafting-table results being taken for the event-trigger system. */
@Mixin(ResultSlot.class)
public abstract class CraftResultServerMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void aromaaffect$onCraft(Player player, ItemStack stack, CallbackInfo ci) {
        ServerEventBusHandler.onItemCrafted(player, stack);
    }
}
