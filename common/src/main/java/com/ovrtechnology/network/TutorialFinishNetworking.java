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

    private static final ResourceLocation CONTINUE_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_demo_continue");

    private static final ResourceLocation TIMER_PACKET =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "tutorial_demo_timer");

    private static boolean initialized = false;

    /** Client-side: seconds left in the session as last sent by the server, 0 when there is no countdown. */
    private static volatile int clientSecondsLeft = 0;

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

        // C2S: the player chose "Continue playing" on the end screen
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CONTINUE_PACKET,
                (buf, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        com.ovrtechnology.tutorial.demo.DemoTimerHandler.continueSession(serverPlayer);
                    }
                }));

        // S2C: seconds left in the session, for the HUD
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TIMER_PACKET,
                (buf, context) -> {
                    int seconds = buf.readVarInt();
                    context.queue(() -> clientSecondsLeft = seconds);
                });

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
     * @param timeExpired true when the session is over (time ran out or End Demo): the screen
     *                    offers to keep playing with a new countdown or to start the next game
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

    /**
     * Client sends this when the visitor chooses to keep playing after the time ran out.
     */
    public static void sendContinueToServer(RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        NetworkManager.sendToServer(CONTINUE_PACKET, buf);
    }

    /**
     * Server tells the player how many seconds the session has left; 0 hides the countdown.
     */
    public static void sendTimer(ServerPlayer player, int secondsLeft) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeVarInt(Math.max(0, secondsLeft));
        NetworkManager.sendToPlayer(player, TIMER_PACKET, buf);
    }

    public static int getClientSecondsLeft() {
        return clientSecondsLeft;
    }

    /** Client-side: forget the countdown, for when the client leaves the world. */
    public static void clearClientTimer() {
        clientSecondsLeft = 0;
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
