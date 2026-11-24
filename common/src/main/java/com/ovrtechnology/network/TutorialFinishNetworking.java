package com.ovrtechnology.network;

import com.ovrtechnology.AromaAffect;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class TutorialFinishNetworking {

    private static final ResourceLocation FINISH_OPEN_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_finish_open");

    private static final ResourceLocation END_DEMO_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_demo_end");

    private static boolean initialized = false;

    private TutorialFinishNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        // S2C: Server tells client to open FinishScreen
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, FINISH_OPEN_PACKET,
                (buf, context) -> {
                    boolean expired = buf.readBoolean();
                    context.queue(() -> openFinishOnClient(expired));
                });

        // C2S: the player chose "End Demo" in the pause menu
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, END_DEMO_PACKET,
                (buf, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        com.ovrtechnology.tutorial.demo.DemoTimerHandler.endSession(serverPlayer);
                    }
                }));

        AromaAffect.LOGGER.debug("Tutorial finish networking initialized");
    }

    /**
     * Server sends this to a player to open the finish screen.
     */
    public static void sendOpenFinish(ServerPlayer player) {
        sendOpenFinish(player, false);
    }

    /**
     * Server sends this to a player to open the finish screen.
     * @param timeExpired if true, hides the "Continue" button (timer ran out)
     */
    public static void sendOpenFinish(ServerPlayer player, boolean timeExpired) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeBoolean(timeExpired);
        NetworkManager.sendToPlayer(player, FINISH_OPEN_PACKET, buf);
    }

    /**
     * Client sends this to the server when the player ends the demo before the time runs out.
     */
    public static void sendEndDemoToServer(RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        NetworkManager.sendToServer(END_DEMO_PACKET, buf);
    }

    private static void openFinishOnClient(boolean timeExpired) {
        try {
            // Set the time expired flag before opening the screen
            Class<?> screenClass = Class.forName(
                    "com.ovrtechnology.tutorial.finishscreen.client.TutorialFinishScreen");
            screenClass.getMethod("setTimeExpired", boolean.class).invoke(null, timeExpired);

            Class<?> clientClass = Class.forName(
                    "com.ovrtechnology.tutorial.finishscreen.client.TutorialFinishClient");
            clientClass.getMethod("open").invoke(null);
        } catch (ReflectiveOperationException e) {
            AromaAffect.LOGGER.error("Failed to open finish screen via network packet: {}", e.getMessage(), e);
        }
    }
}
