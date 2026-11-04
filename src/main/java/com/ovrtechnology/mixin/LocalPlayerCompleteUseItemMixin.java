package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.PlayerItemUseEventDispatcher;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LocalPlayerCompleteUseItemMixin {

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void aromaaffect$onCompleteUsingItem(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof LocalPlayer localPlayer)) return;

        ItemStack using = localPlayer.getUseItem();
        if (using.isEmpty()) return;

        Level level = localPlayer.level();
        if (level == null) return;

        try {
            PlayerItemUseEventDispatcher.onItemUseFinished(localPlayer, using.copy());
        } catch (Throwable t) {
            // Defensive: never propagate to vanilla path
        }
    }
}
