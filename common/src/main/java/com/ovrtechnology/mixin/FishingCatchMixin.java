package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.EventScentManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires the "fishing" event scent hook when a player reels in a catch.
 *
 * <p>{@code retrieve} runs on every reel-in; {@code nibble > 0} means a fish
 * was actually on the line, so we only fire on a real catch. Server-side only;
 * scent/cooldown are data-driven and the client gates on passive mode.</p>
 */
@Mixin(FishingHook.class)
public abstract class FishingCatchMixin {

    @Shadow
    private int nibble;

    @Shadow
    public abstract Player getPlayerOwner();

    @Inject(method = "retrieve", at = @At("HEAD"))
    private void aromaaffect$onRetrieve(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (this.nibble > 0 && getPlayerOwner() instanceof ServerPlayer player) {
            EventScentManager.fire(player, "fishing");
        }
    }
}
