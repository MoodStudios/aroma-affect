package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Detects a glass bottle being filled from a dragon breath cloud. Water bottles take the same path and are ignored. */
@Mixin(BottleItem.class)
public abstract class DragonBreathBottleServerMixin {
    @Inject(method = "turnBottleIntoItem", at = @At("HEAD"), require = 0)
    private void aromaaffect$onBottleFilled(ItemStack bottle, Player player, ItemStack result, CallbackInfoReturnable<ItemStack> cir) {
        if (!(player instanceof ServerPlayer sPlayer)) return;
        if (!result.is(Items.DRAGON_BREATH)) return;

        ServerEventBusHandler.onDragonBreathBottled(sPlayer);
    }
}
