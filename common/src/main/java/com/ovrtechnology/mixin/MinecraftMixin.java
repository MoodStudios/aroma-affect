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

            // Tell the server to stop FIRST (in background).
            // halt() hands the server thread one task and waits for it, so it does not return
            // if that thread is stuck in a tick. The line before it is what tells that case
            // apart in a log: it is then the last thing the game wrote.
            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            com.ovrtechnology.AromaAffect.LOGGER.info("Demo restart: stopping the server");
            if (server != null && server.isRunning()) {
                server.halt(false);
            }

            // Stop taking packets from the server that is going away, as leaving a world
            // normally does. One that arrives after the world is cleared has nothing to
            // apply to and is reported as a network error.
            net.minecraft.client.multiplayer.ClientPacketListener listener = mc.getConnection();
            if (listener != null) {
                listener.close();
            }

            // Clear client state
            if (mc.level != null) {
                mc.clearClientLevel(new net.minecraft.client.gui.screens.GenericMessageScreen(
                        net.minecraft.network.chat.Component.literal("Loading...")));
            }

            DemoWorldManager.beginServerStop(server);
        }

        // The old server is still saving its world for a moment after halt(). Deleting the
        // session folder and copying the template over it before it finishes mixes files of
        // the two games, so the frames in between only show the loading screen.
        if (DemoWorldManager.isWaitingForServerStop() && DemoWorldManager.serverStopFinished()) {
            Minecraft mc = (Minecraft) (Object) this;

            // Clean up old session files
            DemoWorldManager.cleanupSession();
            DemoWorldManager.cleanupEditSession();

            // Go to title screen
            mc.setScreen(new TitleScreen());
        }
    }
}
