package com.ovrtechnology.mixin;

import com.ovrtechnology.tutorial.cinematic.client.CinematicCameraSmoother;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws cinematics from the smoothed camera instead of the teleported player position.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void aromaaffect$smoothCinematicCamera(BlockGetter level, Entity entity, boolean detached,
                                                   boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (entity != Minecraft.getInstance().player || !CinematicCameraSmoother.isActive()) {
            return;
        }
        this.setRotation(CinematicCameraSmoother.yaw(partialTick), CinematicCameraSmoother.pitch(partialTick));
        this.setPosition(
                CinematicCameraSmoother.x(partialTick),
                CinematicCameraSmoother.y(partialTick),
                CinematicCameraSmoother.z(partialTick));
    }
}
