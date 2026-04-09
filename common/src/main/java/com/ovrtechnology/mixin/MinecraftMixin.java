package com.ovrtechnology.mixin;

import com.ovrtechnology.tutorial.demo.DemoWorldManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Processes pending disconnect at the TOP of the game loop,
 * before any events fire. This prevents the deadlock caused by
 * calling disconnect() from inside event handlers.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runTick(Z)V"))
    private void aromaaffect$checkPendingDisconnect(CallbackInfo ci) {
        if (DemoWorldManager.isPendingDisconnect()) {
            DemoWorldManager.clearPendingDisconnect();
            Minecraft mc = (Minecraft) (Object) this;

            // Tell the server to stop FIRST (in background)
            if (mc.getSingleplayerServer() != null && mc.getSingleplayerServer().isRunning()) {
                mc.getSingleplayerServer().halt(false);
            }

            // Clear client state
            if (mc.level != null) {
                mc.clearClientLevel(new net.minecraft.client.gui.screens.GenericMessageScreen(
                        net.minecraft.network.chat.Component.literal("Loading...")));
            }

            // Clean up old session files
            DemoWorldManager.cleanupSession();
            DemoWorldManager.cleanupEditSession();

            // Go to title screen
            mc.setScreen(new TitleScreen());
        }
    }
}
