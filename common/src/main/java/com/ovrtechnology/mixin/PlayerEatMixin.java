package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.EventScentManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires an event scent hook when a player finishes eating/drinking a food item.
 *
 * <p>Kept intentionally minimal: it only detects the completion and delegates
 * to {@link EventScentManager#fireEat}. All classification logic lives in a
 * regular class so its static state initializes lazily at runtime — a static
 * {@code Set.of(Items...)} here would be merged into {@code LivingEntity}'s
 * class initializer and run before the item registry is populated (NPE).</p>
 */
@Mixin(LivingEntity.class)
public abstract class PlayerEatMixin {

    @Shadow
    public abstract ItemStack getUseItem();

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void aromaaffect$onFinishEating(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            EventScentManager.fireEat(player, getUseItem());
        }
    }
}
