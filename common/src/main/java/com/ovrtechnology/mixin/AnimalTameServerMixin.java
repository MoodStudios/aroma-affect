package com.ovrtechnology.mixin;

import com.ovrtechnology.trigger.event.ServerEventBusHandler;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Detects taming of tamable animals for the event-trigger system. */
@Mixin(TamableAnimal.class)
public abstract class AnimalTameServerMixin {

    @Inject(method = "tame", at = @At("TAIL"))
    private void aromaaffect$onTame(Player player, CallbackInfo ci) {
        ServerEventBusHandler.onAnimalTamed((Animal) (Object) this, player);
    }
}
