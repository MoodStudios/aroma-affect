package com.ovrtechnology.mixin;

import com.ovrtechnology.tutorial.cinematic.client.CinematicCameraLock;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks mouse camera rotation during cinematics.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void aromaaffect$blockCameraDuringCinematic(double movementTime, CallbackInfo ci) {
        if (CinematicCameraLock.isLocked()) {
            ci.cancel();
        }
    }
}
