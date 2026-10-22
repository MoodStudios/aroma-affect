package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilOnTakeMixin {

    @Inject(method = "onTake", at = @At("TAIL"))
    private void aromaaffect$onAnvilResultTaken(Player player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;
        ServerEventBusHandler.fireSimpleEvent(serverPlayer, ServerEventBusHandler.TT_ANVIL_USED);
    }
}
