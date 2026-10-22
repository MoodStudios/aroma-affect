package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookRetrieveMixin {

    @Inject(method = "retrieve", at = @At("HEAD"))
    private void aromaaffect$onRetrieve(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        FishingHook self = (FishingHook) (Object) this;
        Player owner = self.getPlayerOwner();
        if (!(owner instanceof ServerPlayer serverPlayer)) return;
        if (self.level().isClientSide()) return;
        if (self.getHookedIn() == null && stack != null && !stack.isEmpty()) {
            ServerEventBusHandler.fireSimpleEvent(
                    serverPlayer, ServerEventBusHandler.TT_FISHING_PULLED);
        }
    }
}
